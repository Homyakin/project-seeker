package ru.homyakin.seeker.game.event.raid;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.raid.models.RaidTemplate;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingRaid;
import ru.homyakin.seeker.utils.JsonUtils;

@Component
public class RaidDao {
    private static final String GET_BY_ID = "SELECT * FROM raid WHERE event_id = :event_id";
    private static final String SAVE = """
        INSERT INTO raid (event_id, template_id, locale)
        VALUES (:event_id, :template_id, :locale)
        ON CONFLICT (event_id)
        DO UPDATE SET template_id = :template_id, locale = :locale
        """;
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public RaidDao(DataSource dataSource, JsonUtils jsonUtils) {
        jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    public Optional<Raid> getByEventId(int eventId) {
        return jdbcClient.sql(GET_BY_ID)
            .param("event_id", eventId)
            .query(this::mapRow)
            .optional();
    }

    public void save(int eventId, SavingRaid raid) {
        jdbcClient.sql(SAVE)
            .param("event_id", eventId)
            .param("template_id", raid.template().id())
            .param("locale", jsonUtils.mapToPostgresJson(raid.locales()))
            .update();
    }

    private Raid mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Raid(
            rs.getInt("event_id"),
            RaidTemplate.get(rs.getInt("template_id")),
            jsonUtils.fromString(rs.getString("locale"), JsonUtils.RAID_LOCALE)
        );
    }
}
