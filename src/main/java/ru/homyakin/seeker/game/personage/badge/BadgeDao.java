package ru.homyakin.seeker.game.personage.badge;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingBadge;
import ru.homyakin.seeker.locale.Language;

@Repository
public class BadgeDao {
    private final JdbcClient jdbcClient;

    public BadgeDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Transactional
    public void save(SavingBadge badge) {
        final var sql = """
        INSERT INTO badge (code)
        VALUES (:code)
        ON CONFLICT (code) DO UPDATE SET code = :code
        RETURNING id
        """;
        final int id = jdbcClient.sql(sql)
            .param("code", badge.view().code())
            .query((rs, rowNum) -> rs.getInt("id"))
            .single();
        badge.locales().forEach(locale -> saveLocale(id, locale));
    }

    public Optional<Badge> getByCode(String code) {
        final var sql = "SELECT * FROM badge WHERE code = :code";
        return jdbcClient.sql(sql)
            .param("code", code)
            .query(this::mapBadge)
            .optional()
            .map(it -> it.toBadge(getLocales(it.id)));
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

    private List<BadgeLocale> getLocales(int badgeId) {
        final var sql = "SELECT * FROM badge_locale WHERE badge_id = :badge_id";
        return jdbcClient.sql(sql)
            .param("badge_id", badgeId)
            .query(this::mapLocale)
            .list();
    }

    private void saveLocale(int badgeId, BadgeLocale locale) {
        final var sql = """
        INSERT INTO badge_locale (badge_id, language_id, description)
        VALUES (:badge_id, :language_id, :description)
        ON CONFLICT (badge_id, language_id)
        DO UPDATE SET description = :description
        """;
        jdbcClient.sql(sql)
            .param("badge_id", badgeId)
            .param("language_id", locale.language().id())
            .param("description", locale.description())
            .update();
    }

    private BadgeWithoutLocale mapBadge(ResultSet rs, int rowNum) throws SQLException {
        return new BadgeWithoutLocale(
            rs.getInt("id"),
            rs.getString("code")
        );
    }

    private BadgeLocale mapLocale(ResultSet rs, int rowNum) throws SQLException {
        return new BadgeLocale(
            Language.getOrDefault(rs.getInt("language_id")),
            rs.getString("description")
        );
    }

    private record BadgeWithoutLocale(
        int id,
        String code
    ) {
        public Badge toBadge(List<BadgeLocale> locales) {
            return new Badge(id, BadgeView.findByCode(code), locales);
        }
    }
}
