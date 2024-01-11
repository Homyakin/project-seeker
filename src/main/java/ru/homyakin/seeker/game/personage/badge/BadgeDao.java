package ru.homyakin.seeker.game.personage.badge;

import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class BadgeDao {
    private final JdbcClient jdbcClient;

    public BadgeDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Transactional
    public void save(Badge badge) {
        final var sql = """
        INSERT INTO badge (code)
        VALUES (:code)
        ON CONFLICT (code) DO NOTHING
        RETURNING id
        """;
        final int id = jdbcClient.sql(sql)
            .param("code", badge.code())
            .query((rs, rowNum) -> rs.getInt("id"))
            .single();
        badge.locales().forEach(locale -> saveLocale(id, locale));
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
}
