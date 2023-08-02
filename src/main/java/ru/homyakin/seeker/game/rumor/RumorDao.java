package ru.homyakin.seeker.game.rumor;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.locale.Language;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class RumorDao {
    private static final String GET_RANDOM = """
        SELECT * FROM rumor WHERE is_available = true ORDER BY random() LIMIT 1
        """;
    private static final String GET_RUMOR_LOCALES = "SELECT * FROM rumor_locale WHERE rumor_id = :rumor_id";
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RumorDao(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public Optional<Rumor> getRandomAvailableRumor() {
        return jdbcTemplate.query(GET_RANDOM, this::mapRumor)
            .stream()
            .findFirst()
            .map(it -> it.toRumor(getRumorLocales(it.id())));
    }

    private List<RumorLocale> getRumorLocales(int rumorId) {
        final var params = Collections.singletonMap("rumor_id", rumorId);
        return jdbcTemplate.query(
            GET_RUMOR_LOCALES,
            params,
            this::mapRumorLocale
        );
    }

    private RumorWithoutLocale mapRumor(ResultSet rs, int rowNum) throws SQLException {
        return new RumorWithoutLocale(
            rs.getInt("id"),
            rs.getString("code")
        );
    }

    private RumorLocale mapRumorLocale(ResultSet rs, int rowNum) throws SQLException {
        return new RumorLocale(
            Language.getOrDefault(rs.getInt("language_id")),
            rs.getString("text")
        );
    }

    private record RumorWithoutLocale(
        int id,
        String code
    ) {
        public Rumor toRumor(List<RumorLocale> locales) {
            return new Rumor(
                id,
                code,
                locales
            );
        }
    }
}
