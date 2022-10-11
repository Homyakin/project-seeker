package ru.homyakin.seeker.game.personage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;

@Component
public class PersonageDao {
    private static final String GET_BY_ID = """
        SELECT * FROM personage WHERE id = :id
        """;
    private static final PersonageRowMapper PERSONAGE_ROW_MAPPER = new PersonageRowMapper();
    private final SimpleJdbcInsert jdbcInsert;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PersonageDao(DataSource dataSource) {
        jdbcInsert = new SimpleJdbcInsert(dataSource)
            .withTableName("personage")
            .usingColumns(
                "level",
                "current_exp"
            );
        jdbcInsert.setGeneratedKeyName("id");

        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public long save(int level, long currentExp) {
        final var params = new HashMap<String, Object>() {{
            put("level", level);
            put("current_exp", currentExp);
        }};
        return jdbcInsert.executeAndReturnKey(
            params
        ).longValue();
    }

    public Optional<Personage> getById(Long id) {
        final var params = Collections.singletonMap("id", id);
        final var result = jdbcTemplate.query(
            GET_BY_ID,
            params,
            PERSONAGE_ROW_MAPPER
        );
        return result.stream().findFirst();
    }

    private static class PersonageRowMapper implements RowMapper<Personage> {

        @Override
        public Personage mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Personage(
                rs.getLong("id"),
                rs.getInt("level"),
                rs.getLong("current_exp")
            );
        }
    }
}
