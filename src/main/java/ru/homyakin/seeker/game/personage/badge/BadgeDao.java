package ru.homyakin.seeker.game.personage.badge;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingBadge;
import ru.homyakin.seeker.locale.Language;
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
            .optional()
            .map(it -> it.toBadge(getLocales(it.id)));
    }

    public Optional<Badge> getById(int id) {
        final var sql = "SELECT * FROM badge WHERE id = :id";
        return jdbcClient.sql(sql)
            .param("id", id)
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

    public List<PersonageAvailableBadge> getPersonageAvailableBadges(PersonageId personageId) {
        final var sql = """
            SELECT * FROM personage_available_badge pab
            LEFT JOIN public.badge b on b.id = pab.badge_id
            WHERE personage_id = :personage_id
            ORDER BY b.id
            """;
        return jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .query((rs, rowNum) -> {
                final var badge = mapBadge(rs, rowNum);
                final var locales = getLocales(badge.id());
                return new PersonageAvailableBadge(
                    personageId,
                    badge.toBadge(locales),
                    rs.getBoolean("is_active")
                );
            })
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

    @SuppressWarnings("unchecked")
    private Map<Language, BadgeLocale> getLocales(int badgeId) {
        final var sql = "SELECT * FROM badge_locale WHERE badge_id = :badge_id";
        final var list = jdbcClient.sql(sql)
            .param("badge_id", badgeId)
            .query(this::mapLocale)
            .list();
        return Map.ofEntries(list.toArray(new Map.Entry[0]));
    }

    private void saveLocale(int badgeId, Language language, BadgeLocale locale) {
        final var sql = """
            INSERT INTO badge_locale (badge_id, language_id, description)
            VALUES (:badge_id, :language_id, :description)
            ON CONFLICT (badge_id, language_id)
            DO UPDATE SET description = :description
            """;
        jdbcClient.sql(sql)
            .param("badge_id", badgeId)
            .param("language_id", language.id())
            .param("description", locale.description())
            .update();
    }

    private BadgeWithoutLocale mapBadge(ResultSet rs, int rowNum) throws SQLException {
        return new BadgeWithoutLocale(
            rs.getInt("id"),
            rs.getString("code")
        );
    }

    private Map.Entry<Language, BadgeLocale> mapLocale(ResultSet rs, int rowNum) throws SQLException {
        return Map.entry(
            Language.getOrDefault(rs.getInt("language_id")),
            new BadgeLocale(rs.getString("description"))
        );
    }

    private record BadgeWithoutLocale(
        int id,
        String code
    ) {
        public Badge toBadge(Map<Language, BadgeLocale> locales) {
            return new Badge(id, BadgeView.findByCode(code), locales);
        }
    }
}
