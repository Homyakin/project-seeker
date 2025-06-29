package ru.homyakin.seeker.game.personage.badge;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingBadge;
import ru.homyakin.seeker.utils.JsonUtils;

@Repository
public class BadgeDao {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public BadgeDao(DataSource dataSource, JsonUtils jsonUtils) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    @Transactional
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

    public Optional<Badge> getByCode(String code) {
        final var sql = "SELECT * FROM badge WHERE code = :code";
        return jdbcClient.sql(sql)
            .param("code", code)
            .query(this::mapBadge)
            .optional();
    }

    public Optional<Badge> getById(int id) {
        final var sql = "SELECT * FROM badge WHERE id = :id";
        return jdbcClient.sql(sql)
            .param("id", id)
            .query(this::mapBadge)
            .optional();
    }

    public void savePersonageAvailableBadge(PersonageAvailableBadge availableBadge) {
        final var sql = """
            INSERT INTO personage_available_badge (personage_id, badge_id, is_active)
            VALUES (:personage_id, :badge_id, :is_active)
            """;
        jdbcClient.sql(sql)
            .param("personage_id", availableBadge.personageId().value())
            .param("badge_id", availableBadge.badge().id())
            .param("is_active", availableBadge.isActive())
            .update();
    }

    public List<PersonageAvailableBadge> getPersonageAvailableBadges(PersonageId personageId) {
        final var sql = """
            SELECT * FROM personage_available_badge pab
            LEFT JOIN public.badge b on b.id = pab.badge_id
            WHERE personage_id = :personage_id
            ORDER BY b.id
            """;
        return jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .query((rs, rowNum) -> new PersonageAvailableBadge(
                personageId,
                mapBadge(rs, rowNum),
                rs.getBoolean("is_active")
            ))
            .list();
    }

    @Transactional
    public void activatePersonageBadge(PersonageId personageId, Badge badge) {
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
            .param("badge_id", badge.id())
            .update();
    }

    private Badge mapBadge(ResultSet rs, int rowNum) throws SQLException {
        return new Badge(
            rs.getInt("id"),
            BadgeView.findByCode(rs.getString("code")),
            jsonUtils.fromString(rs.getString("locale"), JsonUtils.BADGE_LOCALE)
        );
    }
}
