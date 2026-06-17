package ru.homyakin.seeker.telegram.world_raid;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.Language;

import javax.sql.DataSource;
import java.util.List;

@Component
public class TelegramWorldRaidDao {
    private final JdbcClient jdbcClient;

    public TelegramWorldRaidDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    public void save(TelegramWorldRaid worldRaid) {
        final var sql = """
            INSERT INTO world_raid_launched_tg (world_raid_id, channel_id, language_id, message_id)
            VALUES (:event_id, :channel_id, :language_id, :message_id)
            """;
        jdbcClient.sql(sql)
            .param("event_id", worldRaid.worldRaidId())
            .param("channel_id", worldRaid.channelId())
            .param("language_id", worldRaid.language().id())
            .param("message_id", worldRaid.messageId())
            .update();
    }

    public List<TelegramWorldRaid> getByWorldRaidId(long worldRaidId) {
        final var sql = """
            SELECT
                channel_id,
                language_id,
                message_id
            FROM world_raid_launched_tg
            WHERE world_raid_id = :world_raid_id
            """;
        return jdbcClient.sql(sql)
            .param("world_raid_id", worldRaidId)
            .query((rs, _) -> new TelegramWorldRaid(
                worldRaidId,
                rs.getLong("channel_id"),
                Language.getOrDefault(rs.getInt("language_id")),
                rs.getInt("message_id")
            ))
            .list();
    }
}
