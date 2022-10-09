package ru.homyakin.seeker.character;

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
public class CharacterDao {
    private static final String GET_BY_ID = """
        SELECT * FROM character WHERE id = :id
        """;
    private static final CharacterRowMapper CHARACTER_ROW_MAPPER = new CharacterRowMapper();
    private final SimpleJdbcInsert jdbcInsert;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CharacterDao(DataSource dataSource) {
        jdbcInsert = new SimpleJdbcInsert(dataSource)
            .withTableName("character")
            .usingColumns(
                "level",
                "current_exp"
            );
        jdbcInsert.setGeneratedKeyName("id");

        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public long saveCharacter(int level, long currentExp) {
        final var params = new HashMap<String, Object>() {{
            put("level", level);
            put("current_exp", currentExp);
        }};
        return jdbcInsert.executeAndReturnKey(
            params
        ).longValue();
    }

    public Optional<Character> getById(Long id) {
        final var params = Collections.singletonMap("id", id);
        final var result = jdbcTemplate.query(
            GET_BY_ID,
            params,
            CHARACTER_ROW_MAPPER
        );
        return result.stream().findFirst();
    }

    private static class CharacterRowMapper implements RowMapper<Character> {

        @Override
        public Character mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Character(
                rs.getLong("id"),
                rs.getInt("level"),
                rs.getLong("current_exp")
            );
        }
    }
}
