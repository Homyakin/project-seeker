package ru.homyakin.seeker.game.personage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.models.Personage;

@Component
public class PersonageDao {
    private static final String GET_BY_ID = """
        SELECT * FROM personage WHERE id = :id
        """;
    private static final String GET_BY_BOSS_EVENT = """
        SELECT p.* FROM boss b
        LEFT JOIN personage p on p.id = b.personage_id
        WHERE b.event_id = :event_id
        """;

    private static final String GET_BY_LAUNCHED_EVENT = """
        SELECT p.* FROM personage_to_event le
        LEFT JOIN personage p on p.id = le.personage_id
        WHERE le.launched_event_id = :launched_event_id
        """;

    private static final String GET_TOP_BY_EXP_IN_GROUP = """
        SELECT p.* FROM group_to_user gtu
        LEFT JOIN tg_user tu on tu.id = gtu.user_id
        LEFT JOIN personage p on p.id = tu.personage_id
        WHERE gtu.group_id = :group_id
        ORDER BY p.current_exp DESC
        LIMIT :count
        """;

    private static final String GET_PERSONAGE_POSITION_IN_TOP_BY_EXP_IN_GROUP = """
        WITH numbered_personage as (
        SELECT row_number() over (ORDER BY p.current_exp DESC) as number, p.id
        FROM group_to_user gtu
        LEFT JOIN tg_user tu on tu.id = gtu.user_id
        LEFT JOIN personage p on p.id = tu.personage_id
        WHERE gtu.group_id = :group_id
        ORDER BY p.current_exp DESC
        )
        SELECT number
        FROM numbered_personage
        WHERE id = :id
        """;
    private static final String UPDATE = """
        UPDATE personage
        SET level = :level, current_exp = :current_exp, name = :name,
        strength = :strength, agility = :agility, wisdom = :wisdom,
        health = :health, last_health_change = :last_health_change
        WHERE id = :id
        """;

    private static final PersonageRowMapper PERSONAGE_ROW_MAPPER = new PersonageRowMapper();
    private final SimpleJdbcInsert jdbcInsert;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PersonageDao(DataSource dataSource) {
        jdbcInsert = new SimpleJdbcInsert(dataSource)
            .withTableName("personage")
            .usingColumns(
                "name",
                "level",
                "current_exp",
                "attack",
                "defense",
                "health",
                "strength",
                "agility",
                "wisdom",
                "last_health_change"
            );
        jdbcInsert.setGeneratedKeyName("id");

        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public long save(Personage personage) {
        final var params = new HashMap<String, Object>() {{
            put("name", personage.name());
            put("level", personage.level());
            put("current_exp", personage.currentExp());
            put("attack", personage.attack());
            put("defense", personage.defense());
            put("health", personage.health());
            put("strength", personage.strength());
            put("agility", personage.agility());
            put("wisdom", personage.wisdom());
            put("last_health_change", personage.lastHealthChange());
        }};
        return jdbcInsert.executeAndReturnKey(
            params
        ).longValue();
    }

    public void update(Personage personage) {
        final var params = new HashMap<String, Object>() {{
            put("id", personage.id());
            put("level", personage.level());
            put("current_exp", personage.currentExp());
            put("name", personage.name());
            put("strength", personage.strength());
            put("agility", personage.agility());
            put("wisdom", personage.wisdom());
            put("health", personage.health());
            put("last_health_change", personage.lastHealthChange());
        }};
        jdbcTemplate.update(
            UPDATE,
            params
        );
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

    public Optional<Personage> getByBossEvent(Long eventId) {
        final var params = Collections.singletonMap("event_id", eventId);
        final var result = jdbcTemplate.query(
            GET_BY_BOSS_EVENT,
            params,
            PERSONAGE_ROW_MAPPER
        );
        return result.stream().findFirst();
    }

    public List<Personage> getByLaunchedEvent(Long launchedEventId) {
        final var params = Collections.singletonMap("launched_event_id", launchedEventId);
        return jdbcTemplate.query(
            GET_BY_LAUNCHED_EVENT,
            params,
            PERSONAGE_ROW_MAPPER
        );
    }

    public List<Personage> getTopByExpInGroup(long groupId, int count) {
        final var params = new HashMap<String, Object>() {{
            put("count", count);
            put("group_id", groupId);
        }};
        return jdbcTemplate.query(
            GET_TOP_BY_EXP_IN_GROUP,
            params,
            PERSONAGE_ROW_MAPPER
        );
    }

    public Optional<Long> getPersonagePositionInTopByExpInGroup(long id, long groupId) {
        final var params = new HashMap<String, Object>() {{
            put("id", id);
            put("group_id", groupId);
        }};
        final var result = jdbcTemplate.query(
            GET_PERSONAGE_POSITION_IN_TOP_BY_EXP_IN_GROUP,
            params,
            (rs, rowNum) -> rs.getLong("number")
        );
        return result.stream().findFirst();
    }

    private static class PersonageRowMapper implements RowMapper<Personage> {

        @Override
        public Personage mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Personage(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getInt("level"),
                rs.getLong("current_exp"),
                rs.getInt("health"),
                rs.getInt("attack"),
                rs.getInt("defense"),
                rs.getInt("strength"),
                rs.getInt("agility"),
                rs.getInt("wisdom"),
                rs.getTimestamp("last_health_change").toLocalDateTime()
            );
        }
    }
}
