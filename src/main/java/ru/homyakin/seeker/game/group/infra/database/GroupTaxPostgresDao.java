package ru.homyakin.seeker.game.group.infra.database;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupTaxInfo;
import ru.homyakin.seeker.game.group.entity.GroupTaxStorage;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class GroupTaxPostgresDao implements GroupTaxStorage {
    private final JdbcClient jdbcClient;

    public GroupTaxPostgresDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(new NamedParameterJdbcTemplate(dataSource));
    }

    @Override
    public GroupTaxInfo loadTaxRow(GroupId groupId) {
        final var sql = """
            SELECT p.effective_tax, p.last_tax_update,
                   (SELECT COUNT(*)::int FROM personage pe WHERE pe.member_pgroup_id = p.id) AS member_count
            FROM pgroup p
            WHERE p.id = :id
            """;
        return jdbcClient.sql(sql)
            .param("id", groupId.value())
            .query((rs, _) -> mapTaxRow(rs))
            .single();
    }

    @Override
    public GroupTaxInfo lockTax(GroupId groupId) {
        final var sql = """
            SELECT p.effective_tax, p.last_tax_update,
                   (SELECT COUNT(*)::int FROM personage pe WHERE pe.member_pgroup_id = p.id) AS member_count
            FROM pgroup p
            WHERE p.id = :id
            FOR UPDATE OF p
            """;
        return jdbcClient.sql(sql)
            .param("id", groupId.value())
            .query((rs, _) -> mapTaxRow(rs))
            .single();
    }

    @Override
    public int countLeaved(GroupId groupId) {
        final var sql = """
            SELECT COUNT(*) FROM group_leaved_members WHERE pgroup_id = :id
            """;
        return jdbcClient.sql(sql)
            .param("id", groupId.value())
            .query((rs, _) -> rs.getInt(1))
            .single();
    }

    @Override
    public void deleteOldestLeaved(GroupId groupId, int deleteCount) {
        if (deleteCount <= 0) {
            return;
        }
        final var sql = """
            DELETE FROM group_leaved_members glm
            USING (
                SELECT id FROM group_leaved_members
                WHERE pgroup_id = :id
                ORDER BY leaved_at ASC
                LIMIT :limit
            ) sub
            WHERE glm.id = sub.id
            """;
        jdbcClient.sql(sql)
            .param("id", groupId.value())
            .param("limit", deleteCount)
            .update();
    }

    @Override
    public void updateTaxRow(GroupId groupId, int effectiveTax, Optional<LocalDateTime> lastTaxUpdate) {
        final var sql = """
            UPDATE pgroup
            SET effective_tax = :effective_tax,
                last_tax_update = :last_tax_update
            WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("id", groupId.value())
            .param("effective_tax", effectiveTax)
            .param("last_tax_update", lastTaxUpdate.orElse(null))
            .update();
    }

    @Override
    public boolean deleteLeavedIfExists(GroupId groupId, PersonageId personageId) {
        final var sql = """
            DELETE FROM group_leaved_members
            WHERE pgroup_id = :gid AND personage_id = :pid
            """;
        int updated = jdbcClient.sql(sql)
            .param("gid", groupId.value())
            .param("pid", personageId.value())
            .update();
        return updated > 0;
    }

    @Override
    public void deleteAllLeaved(GroupId groupId) {
        final var sql = """
            DELETE FROM group_leaved_members WHERE pgroup_id = :id
            """;
        jdbcClient.sql(sql)
            .param("id", groupId.value())
            .update();
    }

    @Override
    public void insertLeaved(GroupId groupId, PersonageId personageId, LocalDateTime leavedAt) {
        final var sql = """
            INSERT INTO group_leaved_members (pgroup_id, personage_id, leaved_at)
            VALUES (:gid, :pid, :leaved_at)
            """;
        jdbcClient.sql(sql)
            .param("gid", groupId.value())
            .param("pid", personageId.value())
            .param("leaved_at", leavedAt)
            .update();
    }

    @Override
    public List<GroupId> findGroupIdsDueForTaxUpdate(LocalDateTime cutoff) {
        final var sql = """
            SELECT p.id FROM pgroup p
            WHERE (p.last_tax_update IS NULL OR p.last_tax_update < :cutoff)
              AND (
                p.effective_tax > 0
                OR EXISTS (SELECT 1 FROM personage pe WHERE pe.member_pgroup_id = p.id)
              )
            """;
        return jdbcClient.sql(sql)
            .param("cutoff", cutoff)
            .query((rs, _) -> GroupId.from(rs.getLong("id")))
            .list();
    }

    private static GroupTaxInfo mapTaxRow(ResultSet rs) throws SQLException {
        return new GroupTaxInfo(
            rs.getInt("effective_tax"),
            rs.getInt("member_count"),
            Optional.ofNullable(rs.getObject("last_tax_update", LocalDateTime.class))
        );
    }
}
