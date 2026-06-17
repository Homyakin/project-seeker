package ru.homyakin.seeker.game.event.personal_quest;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuest;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingPersonalQuest;
import ru.homyakin.seeker.utils.JsonUtils;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Component
public class PersonalQuestDao {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public PersonalQuestDao(DataSource dataSource, JsonUtils jsonUtils) {
        jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    public void save(int eventId, SavingPersonalQuest quest) {
        final var sql = """
            INSERT INTO personal_quest (event_id, locale)
            VALUES (:event_id, :locale)
            ON CONFLICT (event_id)
            DO UPDATE SET locale = :locale
            """;
        jdbcClient.sql(sql)
            .param("event_id", eventId)
            .param("locale", jsonUtils.mapToPostgresJson(quest.locales()))
            .update();
    }

    public Optional<PersonalQuest> getRandomQuest() {
        final var sql = """
            SELECT pq.*, e.code FROM personal_quest pq
            LEFT JOIN event e ON e.id = pq.event_id
            WHERE e.is_enabled = true
            ORDER BY random() LIMIT 1
            """;
        return jdbcClient.sql(sql)
            .query(this::mapRow)
            .optional();
    }

    public Optional<PersonalQuest> getByEventId(int eventId) {
        final var sql = """
            SELECT pq.*, e.code FROM personal_quest pq
            LEFT JOIN event e ON e.id = pq.event_id
            WHERE pq.event_id = :event_id
            """;
        return jdbcClient.sql(sql)
            .param("event_id", eventId)
            .query(this::mapRow)
            .optional();
    }

    private PersonalQuest mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new PersonalQuest(
            rs.getInt("event_id"),
            rs.getString("code"),
            jsonUtils.fromString(rs.getString("locale"), JsonUtils.PERSONAL_QUEST_LOCALE)
        );
    }
}
