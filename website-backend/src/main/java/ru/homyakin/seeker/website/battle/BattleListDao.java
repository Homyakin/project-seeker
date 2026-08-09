package ru.homyakin.seeker.website.battle;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class BattleListDao {
    private static final int EVENT_TYPE_RAID = 1;
    private static final int EVENT_TYPE_WORLD_RAID = 3;
    private static final int EVENT_TYPE_DUEL = 4;
    private static final int EVENT_TYPE_ANOMALY = 5;
    private static final int STATUS_SUCCESS = 3;

    private final JdbcClient jdbcClient;

    public BattleListDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    public List<BattleListItem> findBattles(
        EnumSet<BattleType> types,
        Optional<String> groupFilter,
        Optional<Long> beforeId,
        int limit
    ) {
        final var rows = jdbcClient
            .sql("""
                SELECT
                    le.id AS launched_event_id,
                    le.end_date,
                    le.status_id,
                    e.type_id,
                    a.mode AS anomaly_mode,
                    a.pgroup_id AS anomaly_pgroup_id,
                    a.opponent_pgroup_id,
                    a.winner_pgroup_id,
                    d.initiating_personage_id,
                    d.accepting_personage_id,
                    d.winner_personage_id
                FROM event_battle_log ebl
                JOIN launched_event le ON le.id = ebl.launched_event_id
                JOIN event e ON e.id = le.event_id
                LEFT JOIN anomaly a ON a.launched_event_id = le.id
                LEFT JOIN duel d ON d.launched_event_id = le.id
                WHERE (
                    (e.type_id = :raid_type AND CAST(:include_raid AS BOOLEAN))
                    OR (e.type_id = :world_raid_type AND CAST(:include_world_raid AS BOOLEAN))
                    OR (e.type_id = :duel_type AND CAST(:include_duel AS BOOLEAN))
                    OR (e.type_id = :anomaly_type AND a.mode = 'SAFE' AND CAST(:include_anomaly_pve AS BOOLEAN))
                    OR (e.type_id = :anomaly_type AND a.mode = 'DANGEROUS' AND CAST(:include_anomaly_pvp AS BOOLEAN))
                )
                AND (
                    CAST(:before_id AS BIGINT) IS NULL
                    OR le.id < CAST(:before_id AS BIGINT)
                )
                AND (
                    CAST(:group_filter AS TEXT) IS NULL
                    OR EXISTS (
                        SELECT 1
                        FROM pgroup pg
                        WHERE (
                            (
                                e.type_id IN (:raid_type, :duel_type)
                                AND EXISTS (
                                    SELECT 1
                                    FROM launched_event_to_pgroup letp
                                    WHERE letp.launched_event_id = le.id
                                      AND letp.pgroup_id = pg.id
                                )
                            )
                            OR (
                                e.type_id = :world_raid_type
                                AND EXISTS (
                                    SELECT 1
                                    FROM pgroup_battle_result pbr
                                    WHERE pbr.launched_event_id = le.id
                                      AND pbr.pgroup_id = pg.id
                                )
                            )
                            OR (
                                e.type_id = :duel_type
                                AND EXISTS (
                                    SELECT 1
                                    FROM personage p
                                    WHERE p.id IN (d.initiating_personage_id, d.accepting_personage_id)
                                      AND p.member_pgroup_id = pg.id
                                )
                            )
                            OR (
                                e.type_id = :anomaly_type
                                AND (
                                    pg.id = a.pgroup_id
                                    OR pg.id = a.opponent_pgroup_id
                                )
                            )
                        )
                        AND (
                            pg.tag ILIKE '%' || CAST(:group_filter AS TEXT) || '%'
                            OR pg.name ILIKE '%' || CAST(:group_filter AS TEXT) || '%'
                        )
                    )
                )
                ORDER BY le.id DESC
                LIMIT :limit
                """)
            .param("raid_type", EVENT_TYPE_RAID)
            .param("world_raid_type", EVENT_TYPE_WORLD_RAID)
            .param("duel_type", EVENT_TYPE_DUEL)
            .param("anomaly_type", EVENT_TYPE_ANOMALY)
            .param("include_raid", types.contains(BattleType.RAID))
            .param("include_world_raid", types.contains(BattleType.WORLD_RAID))
            .param("include_duel", types.contains(BattleType.DUEL))
            .param("include_anomaly_pve", types.contains(BattleType.ANOMALY_PVE))
            .param("include_anomaly_pvp", types.contains(BattleType.ANOMALY_PVP))
            .param("before_id", beforeId.orElse(null))
            .param("group_filter", groupFilter.filter(s -> !s.isBlank()).orElse(null))
            .param("limit", limit)
            .query((rs, rowNum) -> new BattleRow(
                rs.getLong("launched_event_id"),
                toLocalDateTime(rs.getTimestamp("end_date")),
                rs.getInt("status_id"),
                rs.getInt("type_id"),
                rs.getString("anomaly_mode"),
                optionalLong(rs.getObject("anomaly_pgroup_id")),
                optionalLong(rs.getObject("opponent_pgroup_id")),
                optionalLong(rs.getObject("winner_pgroup_id")),
                optionalLong(rs.getObject("initiating_personage_id")),
                optionalLong(rs.getObject("accepting_personage_id")),
                optionalLong(rs.getObject("winner_personage_id"))
            ))
            .list();

        if (rows.isEmpty()) {
            return List.of();
        }

        final var loaded = loadParticipants(rows);
        final var result = new ArrayList<BattleListItem>(rows.size());
        for (final var row : rows) {
            toItem(row, loaded).ifPresent(result::add);
        }
        return result;
    }

    private LoadedParticipants loadParticipants(List<BattleRow> rows) {
        final var raidEventIds = new ArrayList<Long>();
        final var worldRaidEventIds = new ArrayList<Long>();
        final var groupIds = new HashSet<Long>();
        final var personageIds = new HashSet<Long>();

        for (final var row : rows) {
            switch (row.typeId()) {
                case EVENT_TYPE_RAID -> raidEventIds.add(row.launchedEventId());
                case EVENT_TYPE_WORLD_RAID -> worldRaidEventIds.add(row.launchedEventId());
                case EVENT_TYPE_DUEL -> {
                    row.initiatingPersonageId().ifPresent(personageIds::add);
                    row.acceptingPersonageId().ifPresent(personageIds::add);
                    row.winnerPersonageId().ifPresent(personageIds::add);
                }
                case EVENT_TYPE_ANOMALY -> {
                    row.anomalyPgroupId().ifPresent(groupIds::add);
                    row.opponentPgroupId().ifPresent(groupIds::add);
                    row.winnerPgroupId().ifPresent(groupIds::add);
                }
                default -> {
                }
            }
        }

        final var groupsById = loadGroupsById(groupIds);
        final var personagesById = loadPersonagesById(personageIds);
        final var groupsByEventId = new HashMap<Long, List<GroupInfo>>();

        if (!raidEventIds.isEmpty()) {
            loadGroupsFromJunction(raidEventIds, groupsByEventId, groupsById);
        }
        if (!worldRaidEventIds.isEmpty()) {
            loadGroupsFromBattleResult(worldRaidEventIds, groupsByEventId, groupsById);
        }

        for (final var row : rows) {
            if (row.typeId() != EVENT_TYPE_ANOMALY) {
                continue;
            }
            final var groups = new ArrayList<GroupInfo>();
            row.anomalyPgroupId().map(groupsById::get).ifPresent(groups::add);
            row.opponentPgroupId().map(groupsById::get).ifPresent(groups::add);
            groupsByEventId.put(row.launchedEventId(), groups);
        }

        return new LoadedParticipants(groupsByEventId, groupsById, personagesById);
    }

    private void loadGroupsFromJunction(
        List<Long> eventIds,
        Map<Long, List<GroupInfo>> groupsByEventId,
        Map<Long, GroupInfo> groupsById
    ) {
        jdbcClient
            .sql("""
                SELECT letp.launched_event_id, pg.id, pg.tag, pg.name
                FROM launched_event_to_pgroup letp
                JOIN pgroup pg ON pg.id = letp.pgroup_id
                WHERE letp.launched_event_id IN (:ids)
                ORDER BY letp.launched_event_id, pg.id
                """)
            .param("ids", eventIds)
            .query((rs, rowNum) -> {
                addGroupRow(rs.getLong("launched_event_id"), rs, groupsByEventId, groupsById);
                return null;
            })
            .list();
    }

    private void loadGroupsFromBattleResult(
        List<Long> eventIds,
        Map<Long, List<GroupInfo>> groupsByEventId,
        Map<Long, GroupInfo> groupsById
    ) {
        jdbcClient
            .sql("""
                SELECT pbr.launched_event_id, pg.id, pg.tag, pg.name
                FROM pgroup_battle_result pbr
                JOIN pgroup pg ON pg.id = pbr.pgroup_id
                WHERE pbr.launched_event_id IN (:ids)
                ORDER BY pbr.launched_event_id, pg.id
                """)
            .param("ids", eventIds)
            .query((rs, rowNum) -> {
                addGroupRow(rs.getLong("launched_event_id"), rs, groupsByEventId, groupsById);
                return null;
            })
            .list();
    }

    private void addGroupRow(
        long eventId,
        ResultSet rs,
        Map<Long, List<GroupInfo>> groupsByEventId,
        Map<Long, GroupInfo> groupsById
    ) throws SQLException {
        final var group = new GroupInfo(
            rs.getLong("id"),
            GroupInfo.formatDisplayName(rs.getString("tag"), rs.getString("name"))
        );
        groupsByEventId.computeIfAbsent(eventId, ignored -> new ArrayList<>()).add(group);
        groupsById.putIfAbsent(group.id(), group);
    }

    private Map<Long, GroupInfo> loadGroupsById(Collection<Long> groupIds) {
        final var result = new LinkedHashMap<Long, GroupInfo>();
        if (groupIds.isEmpty()) {
            return result;
        }
        jdbcClient
            .sql("""
                SELECT id, tag, name
                FROM pgroup
                WHERE id IN (:ids)
                """)
            .param("ids", List.copyOf(groupIds))
            .query((rs, rowNum) -> {
                final var id = rs.getLong("id");
                result.put(id, new GroupInfo(
                    id,
                    GroupInfo.formatDisplayName(rs.getString("tag"), rs.getString("name"))
                ));
                return null;
            })
            .list();
        return result;
    }

    private Map<Long, PersonageInfo> loadPersonagesById(Collection<Long> personageIds) {
        final var result = new LinkedHashMap<Long, PersonageInfo>();
        if (personageIds.isEmpty()) {
            return result;
        }
        jdbcClient
            .sql("""
                SELECT p.id, p.name, pg.tag
                FROM personage p
                LEFT JOIN pgroup pg ON pg.id = p.member_pgroup_id
                WHERE p.id IN (:ids)
                """)
            .param("ids", List.copyOf(personageIds))
            .query((rs, rowNum) -> {
                final var id = rs.getLong("id");
                result.put(id, new PersonageInfo(
                    id,
                    PersonageInfo.formatDisplayName(rs.getString("tag"), rs.getString("name"))
                ));
                return null;
            })
            .list();
        return result;
    }

    private Optional<BattleListItem> toItem(BattleRow row, LoadedParticipants loaded) {
        final var type = resolveType(row);
        if (type.isEmpty()) {
            return Optional.empty();
        }
        final var groups = loaded.groupsByEventId().getOrDefault(row.launchedEventId(), List.of());
        final var pveResult = row.statusId() == STATUS_SUCCESS ? BattleResult.SUCCESS : BattleResult.FAILED;

        return Optional.of(switch (type.get()) {
            case RAID -> new BattleListItem.Raid(
                row.launchedEventId(), groups, pveResult, row.endDate()
            );
            case WORLD_RAID -> new BattleListItem.WorldRaid(
                row.launchedEventId(), groups, pveResult, row.endDate()
            );
            case ANOMALY_PVE -> new BattleListItem.AnomalyPve(
                row.launchedEventId(), groups, pveResult, row.endDate()
            );
            case ANOMALY_PVP -> new BattleListItem.AnomalyPvp(
                row.launchedEventId(),
                groups,
                row.winnerPgroupId().map(loaded.groupsById()::get).orElse(null),
                row.endDate()
            );
            case DUEL -> {
                final var personages = new ArrayList<PersonageInfo>();
                row.initiatingPersonageId().map(loaded.personagesById()::get).ifPresent(personages::add);
                row.acceptingPersonageId().map(loaded.personagesById()::get).ifPresent(personages::add);
                yield new BattleListItem.Duel(
                    row.launchedEventId(),
                    personages,
                    row.winnerPersonageId().map(loaded.personagesById()::get).orElse(null),
                    row.endDate()
                );
            }
        });
    }

    private Optional<BattleType> resolveType(BattleRow row) {
        return switch (row.typeId()) {
            case EVENT_TYPE_RAID -> Optional.of(BattleType.RAID);
            case EVENT_TYPE_WORLD_RAID -> Optional.of(BattleType.WORLD_RAID);
            case EVENT_TYPE_DUEL -> Optional.of(BattleType.DUEL);
            case EVENT_TYPE_ANOMALY -> {
                if ("SAFE".equals(row.anomalyMode())) {
                    yield Optional.of(BattleType.ANOMALY_PVE);
                }
                if ("DANGEROUS".equals(row.anomalyMode())) {
                    yield Optional.of(BattleType.ANOMALY_PVP);
                }
                yield Optional.empty();
            }
            default -> Optional.empty();
        };
    }

    private record LoadedParticipants(
        Map<Long, List<GroupInfo>> groupsByEventId,
        Map<Long, GroupInfo> groupsById,
        Map<Long, PersonageInfo> personagesById
    ) {
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static Optional<Long> optionalLong(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(((Number) value).longValue());
    }

    private record BattleRow(
        long launchedEventId,
        LocalDateTime endDate,
        int statusId,
        int typeId,
        String anomalyMode,
        Optional<Long> anomalyPgroupId,
        Optional<Long> opponentPgroupId,
        Optional<Long> winnerPgroupId,
        Optional<Long> initiatingPersonageId,
        Optional<Long> acceptingPersonageId,
        Optional<Long> winnerPersonageId
    ) {
    }
}
