package ru.homyakin.seeker.website.battle;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BattleListItem.Raid.class, name = "RAID"),
    @JsonSubTypes.Type(value = BattleListItem.WorldRaid.class, name = "WORLD_RAID"),
    @JsonSubTypes.Type(value = BattleListItem.AnomalyPve.class, name = "ANOMALY_PVE"),
    @JsonSubTypes.Type(value = BattleListItem.AnomalyPvp.class, name = "ANOMALY_PVP"),
    @JsonSubTypes.Type(value = BattleListItem.Duel.class, name = "DUEL"),
})
public sealed interface BattleListItem {

    long launchedEventId();

    LocalDateTime endDate();

    record Raid(
        long launchedEventId,
        List<GroupInfo> groups,
        BattleResult result,
        LocalDateTime endDate
    ) implements BattleListItem {
    }

    record WorldRaid(
        long launchedEventId,
        List<GroupInfo> groups,
        BattleResult result,
        LocalDateTime endDate
    ) implements BattleListItem {
    }

    record AnomalyPve(
        long launchedEventId,
        List<GroupInfo> groups,
        BattleResult result,
        LocalDateTime endDate
    ) implements BattleListItem {
    }

    record AnomalyPvp(
        long launchedEventId,
        List<GroupInfo> groups,
        GroupInfo winner,
        LocalDateTime endDate
    ) implements BattleListItem {
    }

    record Duel(
        long launchedEventId,
        List<PersonageInfo> personages,
        PersonageInfo winner,
        LocalDateTime endDate
    ) implements BattleListItem {
    }
}
