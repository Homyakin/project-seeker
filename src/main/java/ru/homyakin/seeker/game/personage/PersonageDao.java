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
import ru.homyakin.seeker.utils.TimeUtils;

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

    private static final String GET_TOP_BY_EXP_IN_CHAT = """
        SELECT p.* FROM chat_to_tg_user cttu
        LEFT JOIN tg_user tu on tu.id = cttu.tg_user_id
        LEFT JOIN personage p on p.id = tu.personage_id
        WHERE cttu.chat_id = :chat_id
        ORDER BY p.current_exp DESC
        LIMIT :count
        """;

    private static final String GET_PERSONAGE_POSITION_IN_TOP_BY_EXP_IN_CHAT = """
        WITH numbered_personage as (
        SELECT row_number() over (ORDER BY p.current_exp DESC) as number, p.id
        FROM chat_to_tg_user cttu
        LEFT JOIN tg_user tu on tu.id = cttu.tg_user_id
        LEFT JOIN personage p on p.id = tu.personage_id
        WHERE cttu.chat_id = :chat_id
        ORDER BY p.current_exp DESC
        )
        SELECT number
        FROM numbered_personage
        WHERE id = :id
        """;
    private static final String UPDATE = """
        UPDATE personage
        SET level = :level, current_exp = :current_exp, name = :name
        WHERE id = :id
        """;

    private static final String UPDATE_HEALTH = """
        UPDATE personage
        SET health = :health, last_health_check = :last_health_check
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
                "last_health_check"
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
            put("last_health_check", personage.lastHealthCheck());
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
        }};
        jdbcTemplate.update(
            UPDATE,
            params
        );
    }

    public void updateHealth(long id, int health, LocalDateTime lastHealthCheck) {
        final var params = new HashMap<String, Object>() {{
            put("id", id);
            put("health", health);
            put("last_health_check", lastHealthCheck);
        }};
        jdbcTemplate.update(
            UPDATE_HEALTH,
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

    public List<Personage> getTopByExpInChat(long chatId, int count) {
        final var params = new HashMap<String, Object>() {{
            put("count", count);
            put("chat_id", chatId);
        }};
        return jdbcTemplate.query(
            GET_TOP_BY_EXP_IN_CHAT,
            params,
            PERSONAGE_ROW_MAPPER
        );
    }

    public Optional<Long> getPersonagePositionInTopByExpInChat(long id, long chatId) {
        final var params = new HashMap<String, Object>() {{
            put("id", id);
            put("chat_id", chatId);
        }};
        final var result = jdbcTemplate.query(
            GET_PERSONAGE_POSITION_IN_TOP_BY_EXP_IN_CHAT,
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
                rs.getTimestamp("last_health_check").toLocalDateTime()
            );
        }
    }
}
