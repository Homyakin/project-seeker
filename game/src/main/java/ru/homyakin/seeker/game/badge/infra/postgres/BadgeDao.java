package ru.homyakin.seeker.game.badge.infra.postgres;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.common.models.BadgeId;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.badge.entity.Badge;
import ru.homyakin.seeker.game.badge.entity.BadgeStorage;
import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.badge.entity.GroupBadgeStorage;
import ru.homyakin.seeker.game.badge.entity.AvailableBadge;
import ru.homyakin.seeker.game.badge.entity.PersonageBadgeStorage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingBadge;
import ru.homyakin.seeker.utils.JsonUtils;

@Repository
public class BadgeDao implements BadgeStorage, PersonageBadgeStorage, GroupBadgeStorage {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public BadgeDao(DataSource dataSource, JsonUtils jsonUtils) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    @Override
    public void save(SavingBadge badge) {
        final var sql = """
            INSERT INTO badge (code, locale)
            VALUES (:code, :locale)
            ON CONFLICT (code) DO UPDATE SET locale = :locale
            """;
        jdbcClient.sql(sql)
            .param("code", badge.view().code())
            .param("locale", jsonUtils.mapToPostgresJson(badge.locales()))
            .update();
    }

    @Override
    public Optional<Badge> getByCode(String code) {
        final var sql = "SELECT * FROM badge WHERE code = :code";
        return jdbcClient.sql(sql)
            .param("code", code)
            .query(this::mapBadge)
            .optional();
    }

    @Override
    public void savePersonageAvailableBadge(PersonageId personageId, BadgeId badgeId, boolean isActive) {
        final var sql = """
            INSERT INTO personage_available_badge (personage_id, badge_id, is_active)
            VALUES (:personage_id, :badge_id, :is_active)
            """;
        jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .param("badge_id", badgeId.value())
            .param("is_active", isActive)
            .update();
    }

    @Override
    public List<AvailableBadge> getPersonageAvailableBadges(PersonageId personageId) {
        final var sql = """
            SELECT * FROM personage_available_badge pab
            LEFT JOIN public.badge b on b.id = pab.badge_id
            WHERE personage_id = :personage_id
            ORDER BY b.id
            """;
        return jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .query((rs, rowNum) -> new AvailableBadge(
                mapBadge(rs, rowNum),
                rs.getBoolean("is_active")
            ))
            .list();
    }

    @Override
    @Transactional
    public void activatePersonageBadge(PersonageId personageId, BadgeId badgeId) {
        final var deactivatePersonageBadges = """
            UPDATE personage_available_badge SET is_active = false
            WHERE personage_id = :personage_id
            AND is_active = true
            """;
        final var activatePersonageBadge = """
            UPDATE personage_available_badge SET is_active = true
            WHERE personage_id = :personage_id
            AND badge_id = :badge_id
            """;
        jdbcClient.sql(deactivatePersonageBadges)
            .param("personage_id", personageId.value())
            .update();
        jdbcClient.sql(activatePersonageBadge)
            .param("personage_id", personageId.value())
            .param("badge_id", badgeId.value())
            .update();
    }

    @Override
    public List<AvailableBadge> getGroupAvailableBadges(GroupId groupId) {
        final var sql = """
            SELECT b.*, p.active_badge_id FROM pgroup_to_badge pb
            LEFT JOIN public.badge b on b.id = pb.badge_id
            LEFT JOIN pgroup p ON p.id = pb.pgroup_id
            WHERE pb.pgroup_id = :pgroup_id
            ORDER BY b.id
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .query((rs, rowNum) -> new AvailableBadge(
                mapBadge(rs, rowNum),
                rs.getInt("id") == rs.getInt("active_badge_id")
            ))
            .list();
    }

    @Override
    public void activateGroupBadge(GroupId groupId, BadgeId badgeId) {
        final var sql = """
            UPDATE pgroup SET active_badge_id = :badge_id WHERE id = :pgroup_id
            """;
        jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("badge_id", badgeId.value())
            .update();
    }

    private Badge mapBadge(ResultSet rs, int rowNum) throws SQLException {
        return new Badge(
            BadgeId.of(rs.getInt("id")),
            BadgeView.findByCode(rs.getString("code")),
            jsonUtils.fromString(rs.getString("locale"), JsonUtils.BADGE_LOCALE)
        );
    }
}
