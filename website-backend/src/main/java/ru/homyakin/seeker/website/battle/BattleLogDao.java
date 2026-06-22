package ru.homyakin.seeker.website.battle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.Optional;

@Repository
public class BattleLogDao {
    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public BattleLogDao(DataSource dataSource, ObjectMapper objectMapper) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.objectMapper = objectMapper;
    }

    public Optional<JsonNode> getInitState(long launchedEventId) {
        return jdbcClient
            .sql("SELECT init FROM event_battle_log WHERE launched_event_id = :id")
            .param("id", launchedEventId)
            .query((rs, rowNum) -> parseJson(rs.getString("init")))
            .optional();
    }

    public Optional<JsonNode> getActionLog(long launchedEventId) {
        return jdbcClient
            .sql("SELECT log FROM event_battle_log WHERE launched_event_id = :id")
            .param("id", launchedEventId)
            .query((rs, rowNum) -> parseJson(rs.getString("log")))
            .optional();
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse JSON from database", e);
        }
    }
}
