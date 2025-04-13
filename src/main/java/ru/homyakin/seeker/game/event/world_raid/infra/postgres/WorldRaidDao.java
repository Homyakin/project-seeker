package ru.homyakin.seeker.game.event.world_raid.infra.postgres;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaid;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaidState;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaidStatus;
import ru.homyakin.seeker.game.event.world_raid.entity.FinalWorldRaidStatus;
import ru.homyakin.seeker.game.event.world_raid.entity.PersonageContribution;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidBattleInfo;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidStorage;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidTemplate;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingWorldRaid;
import ru.homyakin.seeker.utils.JsonUtils;
import ru.homyakin.seeker.utils.TimeUtils;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class WorldRaidDao implements WorldRaidStorage {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public WorldRaidDao(DataSource dataSource, JsonUtils jsonUtils) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        jdbcClient = JdbcClient.create(jdbcTemplate);
        this.jsonUtils = jsonUtils;
    }

    @Override
    public void save(int eventId, SavingWorldRaid raid) {
        final var sql = """
            INSERT INTO world_raid_template (event_id, info, locale)
            VALUES (:event_id, :info, :locale)
            ON CONFLICT (event_id)
            DO UPDATE SET info = :info, locale = :locale
            """;
        jdbcClient.sql(sql)
            .param("event_id", eventId)
            .param("info", jsonUtils.mapToPostgresJson(raid.info()))
            .param("locale", jsonUtils.mapToPostgresJson(raid.locales()))
            .update();
    }

    @Override
    public Optional<ActiveWorldRaid> getActive() {
        final var sql = """
            SELECT
                wrl.id,
                e.code,
                wrl.contribution,
                wrl.required_contribution,
                wrl.info,
                wrl.fund,
                wrl.status_id,
                wrt.locale,
                wrt.event_id,
                wrl.launched_event_id
            FROM world_raid_launched wrl
            LEFT JOIN world_raid_template wrt ON wrl.event_id = wrt.event_id
            LEFT JOIN event e ON wrt.event_id = e.id
            WHERE wrl.status_id in (:research_status_id, :battle_status_id)
            """;
        return jdbcClient.sql(sql)
            .param("research_status_id", ActiveWorldRaidStatus.RESEARCH.id())
            .param("battle_status_id", ActiveWorldRaidStatus.BATTLE.id())
            .query(this::mapActive)
            .optional();
    }

    @Override
    public Optional<WorldRaidTemplate> getRandom() {
        final var sql = """
            SELECT wrt.*, e.code FROM world_raid_template wrt
            LEFT JOIN event e ON wrt.event_id = e.id
            WHERE e.is_enabled = true
            ORDER BY random() LIMIT 1
            """;
        return jdbcClient.sql(sql)
            .query(this::mapTemplate)
            .optional();
    }

    @Override
    public void saveActive(WorldRaidTemplate worldRaidTemplate, Money fund, ActiveWorldRaidState.Research research) {
        jdbcClient.sql(SAVE_LAUNCHED)
            .param("contribution", research.contribution())
            .param("required_contribution", research.requiredContribution())
            .param("info", jsonUtils.mapToPostgresJson(worldRaidTemplate.info()))
            .param("fund", fund.value())
            .param("status_id", ActiveWorldRaidStatus.RESEARCH.id())
            .param("event_id", worldRaidTemplate.eventId())
            .param("start_date", TimeUtils.moscowTime())
            .update();
    }

    @Override
    public void incrementContribution(long id, PersonageId personageId, Money fundIncrease) {
        final var incrementRaidContribution = """
            UPDATE world_raid_launched
            SET contribution = contribution + 1,
                fund = fund + :fund_increase
            WHERE id = :id
            """;
        jdbcClient.sql(incrementRaidContribution)
            .param("fund_increase", fundIncrease.value())
            .param("id", id)
            .update();
        final var incrementPersonageContribution = """
            INSERT INTO world_raid_research
            (personage_id, contribution, world_raid_id)
            VALUES (:personage_id, 1, :world_raid_id)
            ON CONFLICT (personage_id, world_raid_id)
            DO UPDATE SET contribution = world_raid_research.contribution + 1
            """;
        jdbcClient.sql(incrementPersonageContribution)
            .param("personage_id", personageId.value())
            .param("world_raid_id", id)
            .update();
    }

    @Override
    public void setBattleState(long id, long launchedEventId) {
        final var sql = """
            UPDATE world_raid_launched
            SET status_id = :status_id,
            launched_event_id = :launched_event_id
            WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("status_id", ActiveWorldRaidStatus.BATTLE.id())
            .param("launched_event_id", launchedEventId)
            .param("id", id)
            .update();
    }

    @Override
    @Transactional
    public void saveAsContinued(
        ActiveWorldRaid raid,
        WorldRaidBattleInfo info,
        ActiveWorldRaidState.Research research
    ) {
        jdbcClient.sql(SAVE_LAUNCHED)
            .param("contribution", research.contribution())
            .param("required_contribution", research.requiredContribution())
            .param("info", jsonUtils.mapToPostgresJson(info))
            .param("fund", 0)
            .param("status_id", ActiveWorldRaidStatus.RESEARCH.id())
            .param("event_id", raid.eventId())
            .param("start_date", TimeUtils.moscowTime())
            .update();

        setStatus(raid.id(), FinalWorldRaidStatus.CONTINUED);
    }

    @Override
    public void setStatus(long id, FinalWorldRaidStatus status) {
        final var sql = """
            UPDATE world_raid_launched
            SET status_id = :status_id,
            end_date = :end_date
            WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("status_id", status.id())
            .param("id", id)
            .param("end_date", TimeUtils.moscowTime())
            .update();
    }

    @Override
    public List<PersonageContribution> getPersonageContributions(long id) {
        final var sql = """
            SELECT
                personage_id,
                contribution
            FROM world_raid_research
            WHERE world_raid_id = :id
            """;
        return jdbcClient.sql(sql)
            .param("id", id)
            .query((rs, _) -> new PersonageContribution(
                PersonageId.from(rs.getLong("personage_id")),
                rs.getInt("contribution")
            ))
            .list();
    }

    @Override
    public void setRewards(long id, Map<PersonageId, Money> rewards) {
        final var sql = """
            UPDATE world_raid_research
            SET reward = :reward
            WHERE personage_id = :personage_id
            AND world_raid_id = :world_raid_id
            """;
        final var parameters = new ArrayList<SqlParameterSource>();
        for (final var reward : rewards.entrySet()) {
            MapSqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("personage_id", reward.getKey().value())
                .addValue("world_raid_id", id)
                .addValue("reward", reward.getValue().value());
            parameters.add(paramSource);
        }
        jdbcTemplate.batchUpdate(sql, parameters.toArray(new SqlParameterSource[0]));
    }

    @Override
    public void updateFund(long id, Money fund) {
        final var sql = """
            UPDATE world_raid_launched
            SET fund = :fund
            WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("fund", fund.value())
            .param("id", id)
            .update();
    }

    @Override
    public List<GroupId> getRegisteredGroupsToNotify(long worldRaidId, Duration timeout) {
        final var sql = """
            SELECT
                p.id
            FROM pgroup p
            LEFT JOIN world_raid_launched_pgroup wrlpg ON p.id = wrlpg.pgroup_id AND wrlpg.world_raid_id = :world_raid_id
            WHERE
                p.tag IS NOT NULL AND
                (wrlpg.last_notification IS NULL OR wrlpg.last_notification < :last_notification)
            """;
        return jdbcClient.sql(sql)
            .param("world_raid_id", worldRaidId)
            .param("last_notification", TimeUtils.moscowTime().minus(timeout))
            .query((rs, _) -> GroupId.from(rs.getLong("id")))
            .list();
    }

    @Override
    public void updateGroupNotification(long worldRaidId, GroupId groupId, LocalDateTime lastNotification) {
        final var sql = """
            INSERT INTO world_raid_launched_pgroup
            (world_raid_id, pgroup_id, last_notification)
            VALUES (:world_raid_id, :pgroup_id, :last_notification)
            ON CONFLICT (world_raid_id, pgroup_id) DO UPDATE SET last_notification = :last_notification
            """;
        jdbcClient.sql(sql)
            .param("last_notification", lastNotification)
            .param("world_raid_id", worldRaidId)
            .param("pgroup_id", groupId.value())
            .update();
    }

    @Override
    public Optional<Long> getLaunchedEventIdForLastFinished() {
        final var sql = """
            SELECT
                launched_event_id
            FROM world_raid_launched
            WHERE status_id in (:final_status_ids)
            ORDER BY end_date DESC
            LIMIT 1""";
        return jdbcClient.sql(sql)
            .param(
                "final_status_ids",
                Arrays.stream(FinalWorldRaidStatus.values()).map(FinalWorldRaidStatus::id).toList()
            )
            .query((rs, _) -> rs.getLong("launched_event_id"))
            .optional();
    }

    private ActiveWorldRaid mapActive(ResultSet rs, int rowNum) throws SQLException {
        final var status = ActiveWorldRaidStatus.get(rs.getInt("status_id"));
        final var state = switch (status) {
            case RESEARCH -> new ActiveWorldRaidState.Research(
                rs.getInt("contribution"),
                rs.getInt("required_contribution")
            );
            case BATTLE -> new ActiveWorldRaidState.Battle(rs.getLong("launched_event_id"));
        };
        return new ActiveWorldRaid(
            rs.getInt("id"),
            rs.getInt("event_id"),
            rs.getString("code"),
            jsonUtils.fromString(rs.getString("info"), WorldRaidBattleInfo.class),
            Money.from(rs.getInt("fund")),
            state,
            jsonUtils.fromString(rs.getString("locale"), JsonUtils.WORLD_RAID_LOCALE)
        );
    }

    private WorldRaidTemplate mapTemplate(ResultSet rs, int rowNum) throws SQLException {
        return new WorldRaidTemplate(
            rs.getInt("event_id"),
            rs.getString("code"),
            jsonUtils.fromString(rs.getString("info"), WorldRaidBattleInfo.class),
            jsonUtils.fromString(rs.getString("locale"), JsonUtils.WORLD_RAID_LOCALE)
        );
    }

    private static final String SAVE_LAUNCHED = """
        INSERT INTO world_raid_launched
        (contribution, required_contribution, info, fund, status_id, event_id, start_date)
        VALUES (:contribution, :required_contribution, :info, :fund, :status_id, :event_id, :start_date)
        """;
}
