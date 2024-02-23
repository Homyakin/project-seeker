package ru.homyakin.seeker.game.rumor;

import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.locale.Language;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
public class RumorDao {
    private static final String GET_RANDOM = """
        SELECT * FROM rumor WHERE is_available = true ORDER BY random() LIMIT 1
        """;
    private static final String GET_RUMOR_LOCALES = """
        SELECT rl.* FROM rumor_locale rl
        LEFT JOIN public.rumor r on r.groupId = rl.rumor_id
        WHERE r.code = :code
        """;
    private static final String SAVE_RUMOR = """
        INSERT INTO rumor (code, is_available)
        VALUES (:code, :is_available)
        ON CONFLICT (code)
        DO UPDATE SET is_available = :is_available
        RETURNING groupId
        """;
    private static final String SAVE_LOCALE = """
        INSERT INTO rumor_locale (rumor_id, language_id, text)
        VALUES (:rumor_id, :language_id, :text)
        ON CONFLICT (rumor_id, language_id)
        DO UPDATE SET text = :text
        """;
    private final JdbcClient jdbcClient;

    public RumorDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    public Optional<Rumor> getRandomAvailableRumor() {
        return jdbcClient.sql(GET_RANDOM)
            .query(this::mapRumor)
            .optional()
            .map(it -> it.toRumor(getRumorLocales(it.code())));
    }

    @SuppressWarnings("unchecked")
    private Map<Language, RumorLocale> getRumorLocales(String code) {
        final var list = jdbcClient.sql(GET_RUMOR_LOCALES)
            .param("code", code)
            .query(this::mapRumorLocale)
            .list();
        return Map.ofEntries(list.toArray(new Map.Entry[0]));
    }

    @Transactional
    public void saveRumor(Rumor rumor) {
        final int id = jdbcClient.sql(SAVE_RUMOR)
            .param("code", rumor.code())
            .param("is_available", rumor.isAvailable())
            .query((rs, rowNum) -> rs.getInt("id"))
            .single();
        rumor.locales().forEach((language, locale) -> saveLocale(id, language, locale));
    }

    private void saveLocale(int rumorId, Language language, RumorLocale locale) {
        jdbcClient.sql(SAVE_LOCALE)
            .param("rumor_id", rumorId)
            .param("language_id", language.id())
            .param("text", locale.text())
            .update();
    }

    private RumorWithoutLocale mapRumor(ResultSet rs, int rowNum) throws SQLException {
        return new RumorWithoutLocale(
            rs.getString("code"),
            rs.getBoolean("is_available")
        );
    }

    private Map.Entry<Language, RumorLocale> mapRumorLocale(ResultSet rs, int rowNum) throws SQLException {
        return Map.entry(
            Language.getOrDefault(rs.getInt("language_id")),
            new RumorLocale(rs.getString("text"))
        );
    }

    private record RumorWithoutLocale(
        String code,
        boolean isAvailable
    ) {
        public Rumor toRumor(Map<Language, RumorLocale> locales) {
            return new Rumor(
                code,
                isAvailable,
                locales
            );
        }
    }
}
