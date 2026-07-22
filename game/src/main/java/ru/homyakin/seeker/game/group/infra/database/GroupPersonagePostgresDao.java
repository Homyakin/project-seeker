package ru.homyakin.seeker.game.group.infra.database;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.group.entity.personage.GroupMemberLastOnline;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.entity.personage.PersonageMemberGroup;
import ru.homyakin.seeker.game.online.entity.OnlineStreak;
import ru.homyakin.seeker.game.online.entity.PersonageLastOnline;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.DatabaseUtils;

@Repository
public class GroupPersonagePostgresDao implements GroupPersonageStorage {
    private final JdbcClient jdbcClient;

    public GroupPersonagePostgresDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public Optional<PersonageId> randomMember(GroupId groupId) {
        final var sql = """
            SELECT personage_id FROM pgroup_to_personage
            LEFT JOIN personage ON personage.id = pgroup_to_personage.personage_id
            WHERE pgroup_id = :pgroup_id AND is_active = true
            AND personage.member_pgroup_id = :pgroup_id
            ORDER BY random() LIMIT 1;
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .query((rs, _) -> PersonageId.from(rs.getLong("personage_id")))
            .optional();
    }

    @Override
    public int countActivePersonageMembers(GroupId groupId) {
        final var sql = """
            SELECT COUNT(*) FROM personage p
            LEFT JOIN pgroup_to_personage ptp ON ptp.personage_id = p.id AND p.member_pgroup_id = ptp.pgroup_id
            WHERE p.member_pgroup_id = :pgroup_id AND ptp.is_active = true
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .query((rs, _) -> rs.getInt(1))
            .single();
    }

    @Override
    public void deactivatePersonageInGroup(GroupId groupId, PersonageId personageId) {
        final var sql = """
            UPDATE pgroup_to_personage SET is_active = false WHERE pgroup_id = :pgroup_id AND personage_id = :personage_id
            """;
        jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("personage_id", personageId.value())
            .update();
    }

    @Override
    public void createOrActivate(GroupId groupId, PersonageId personageId) {
        final var sql = """
            INSERT INTO pgroup_to_personage (pgroup_id, personage_id, is_active) VALUES (:pgroup_id, :personage_id, true)
            ON CONFLICT (pgroup_id, personage_id) DO UPDATE SET is_active = true
            """;
        jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("personage_id", personageId.value())
            .update();
    }

    @Override
    public void create(GroupId groupId, PersonageId personageId) {
        final var sql = """
            INSERT INTO pgroup_to_personage (pgroup_id, personage_id, is_active) VALUES (:pgroup_id, :personage_id, false)
            ON CONFLICT (pgroup_id, personage_id) DO NOTHING
            """;
        jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("personage_id", personageId.value())
            .update();
    }

    @Override
    public Set<PersonageId> getActiveGroupPersonages(GroupId groupId) {
        final var sql = """
            SELECT personage_id FROM pgroup_to_personage WHERE pgroup_id = :pgroup_id AND is_active = true
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .query((rs, _) -> PersonageId.from(rs.getLong("personage_id")))
            .set();
    }

    @Override
    public List<PersonageLastOnline> listMembersOrderedByPersonageId(GroupId groupId, int offset, int limit) {
        final var sql = """
            SELECT p.id AS personage_id,
             p.last_online AS last_online,
              p.name AS personage_name,
               b.code as badge_code,
               pg.tag as member_tag
            FROM personage p
            LEFT JOIN personage_available_badge pab ON p.id = pab.personage_id AND pab.is_active = true
            LEFT JOIN badge b ON pab.badge_id = b.id
            LEFT JOIN pgroup pg ON p.member_pgroup_id = pg.id
            WHERE p.member_pgroup_id = :pgroup_id
            ORDER BY p.id
            OFFSET :offset LIMIT :limit
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("offset", offset)
            .param("limit", limit)
            .query((rs, _) -> new PersonageLastOnline(
                PersonageId.from(rs.getLong("personage_id")),
                rs.getString("personage_name"),
                Optional.ofNullable(rs.getString("member_tag")),
                BadgeView.findByCode(rs.getString("badge_code")),
                rs.getTimestamp("last_online").toLocalDateTime()
            ))
            .list();
    }

    @Override
    public boolean isPersonageActiveInGroup(GroupId groupId, PersonageId personageId) {
        final var sql = """
            SELECT COUNT(*) FROM pgroup_to_personage
            WHERE pgroup_id = :pgroup_id AND personage_id = :personage_id AND is_active = true
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("personage_id", personageId.value())
            .query((rs, _) -> rs.getInt(1))
            .single() > 0;
    }

    @Override
    public Optional<GroupMemberLastOnline> findActiveMemberLastOnline(GroupId groupId, PersonageId personageId) {
        final var sql = """
            SELECT p.last_online AS personage_last_online,
                   ptp.last_online AS membership_last_online,
                   ptp.online_streak AS membership_online_streak
            FROM personage p
            LEFT JOIN pgroup_to_personage ptp
                ON p.id = ptp.personage_id
                AND ptp.pgroup_id = :pgroup_id
            WHERE p.id = :personage_id
              AND p.member_pgroup_id = :pgroup_id
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("personage_id", personageId.value())
            .query((rs, _) -> {
                final var membershipLastOnline = Optional
                    .ofNullable(rs.getTimestamp("membership_last_online"))
                    .map(Timestamp::toLocalDateTime);
                final Optional<OnlineStreak> membershipStreak;
                if (membershipLastOnline.isEmpty()) {
                    membershipStreak = Optional.empty();
                } else {
                    membershipStreak = Optional.of(new OnlineStreak(
                        rs.getInt("membership_online_streak"),
                        membershipLastOnline.get()
                    ));
                }
                return new GroupMemberLastOnline(
                    rs.getTimestamp("personage_last_online").toLocalDateTime(),
                    membershipLastOnline,
                    membershipStreak
                );
            })
            .optional();
    }

    @Override
    public PersonageMemberGroup getPersonageMemberGroup(PersonageId personageId) {
        final var sql = """
            SELECT member_pgroup_id, member_pgroup_leave_date FROM personage WHERE id = :personage_id;
            """;
        return jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .query((rs, _) ->
                new PersonageMemberGroup(
                    DatabaseUtils.getLongOrEmpty(
                        rs,
                        "member_pgroup_id"
                    ).map(GroupId::from),
                    Optional.ofNullable(
                        rs.getTimestamp("member_pgroup_leave_date")
                    ).map(Timestamp::toLocalDateTime)
                )
            )
            .single();
    }

    @Override
    public void setMemberGroup(PersonageId personageId, GroupId groupId) {
        final var sql = """
            UPDATE personage SET member_pgroup_id = :member_pgroup_id WHERE id = :personage_id;
            """;
        jdbcClient.sql(sql)
            .param("member_pgroup_id", groupId.value())
            .param("personage_id", personageId.value())
            .update();
    }

    @Override
    public void clearMemberGroup(PersonageId personageId, LocalDateTime now) {
        final var sql = """
            UPDATE personage SET
                member_pgroup_id = NULL,
                member_pgroup_leave_date = :now
            WHERE id = :personage_id;
            """;
        jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .param("now", now)
            .update();
    }
}
