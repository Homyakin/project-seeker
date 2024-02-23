package ru.homyakin.seeker.telegram.group.database;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.group.models.Trigger;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Component
public class TriggerDao {

    private static final String GET_BY_GROUP_ID_AND_TEXT_TO_TRIGGER = """
                   select * from triggertg where where group_id = :group_id and text_to_trigger = :text_to_trigger;
            """;

    private static final String UPSERT_NEW_TRIGGER = """
                    insert into triggertg (group_id, text_to_trigger, trigger_text)
                    values (:group_id, :text_to_trigger, :trigger_text)
                    on conflict (PK_triggertg) do update set trigger_text = :trigger_text;
            """;

    private static final String DELETE_TRIGGER = """
                    delete from triggertg where group_id = :group_id and text_to_trigger = :text_to_trigger
            """;

    private final JdbcClient jdbcClient;

    public TriggerDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public Optional<Trigger> getTrigger(Trigger trigger) {
        return jdbcClient.sql(GET_BY_GROUP_ID_AND_TEXT_TO_TRIGGER)
                .param("group_id", trigger.groupId())
                .param("text_to_trigger", trigger.textToTrigger())
                .query(this::mapRow)
                .optional();
    }

    public void createOrUpdate(Trigger trigger) {
        jdbcClient.sql(UPSERT_NEW_TRIGGER)
                .param("group_id", trigger.groupId())
                .param("text_to_trigger", trigger.textToTrigger())
                .param("trigger_text", trigger.triggerText())
                .update();
    }

    public boolean delete(Trigger trigger) {
        return 0 < jdbcClient.sql(DELETE_TRIGGER)
                .param("group_id", trigger.groupId())
                .param("text_to_trigger", trigger.textToTrigger())
                .update();
    }

    private Trigger mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Trigger(
                GroupId.from(rs.getLong("group_id")),
                rs.getString("text_to_trigger"),
                rs.getString("trigger_text")
        );
    }

}
