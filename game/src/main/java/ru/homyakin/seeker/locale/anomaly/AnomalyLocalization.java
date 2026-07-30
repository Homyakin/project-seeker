package ru.homyakin.seeker.locale.anomaly;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.Duration;
import ru.homyakin.seeker.game.event.anomaly.entity.Anomaly;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyError;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyMode;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPersonageResult;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveFormation;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveTemplate;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyReward;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.raid.models.RaidItem;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.personage.event.EventParticipant;
import ru.homyakin.seeker.game.personage.models.PersonageBattleResult;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.locale.battle.BattleLocalization;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;
import ru.homyakin.seeker.utils.TimeUtils;

public final class AnomalyLocalization {
    private static final Resources<AnomalyResource> resources = new Resources<>();

    private AnomalyLocalization() {
    }

    public static void add(Language language, AnomalyResource resource) {
        resources.add(language, resource);
    }

    public static String searchButton(Language language) {
        return resources.getOrDefault(language, AnomalyResource::anomalySearchButton);
    }

    public static String menu(Language language, boolean canStart, boolean isRegistered) {
        final var params = new HashMap<String, Object>();
        params.put("starts_left", canStart ? 1 : 0);
        params.put(
            "scanner_status",
            canStart
                ? resources.getOrDefault(language, AnomalyResource::anomalyMenuScannerReady)
                : resources.getOrDefault(language, AnomalyResource::anomalyMenuScannerRecharging)
        );
        params.put("register_warning", isRegistered ? "" : registerWarning(language));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, AnomalyResource::anomalyMenu),
            params
        );
    }

    private static String registerWarning(Language language) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, AnomalyResource::anomalyMenuRegisterWarning),
            Map.of("register_group_command", CommandType.REGISTER_GROUP.getText())
        );
    }

    public static String gathering(
        Language language,
        Anomaly.Safe safe,
        List<EventParticipant> participants,
        int partySize,
        LaunchedEvent event
    ) {
        return gathering(
            language,
            AnomalyMode.SAFE,
            participants,
            partySize,
            event,
            Optional.of(safe.ownerPersonageId()),
            pveFormat(language, safe.template())
        );
    }

    public static String gathering(
        Language language,
        Anomaly.Dangerous.Gathering gathering,
        List<EventParticipant> participants,
        int partySize,
        LaunchedEvent event
    ) {
        return gathering(
            language,
            AnomalyMode.DANGEROUS,
            participants,
            partySize,
            event,
            Optional.of(gathering.ownerPersonageId()),
            ""
        );
    }

    public static String pveWaiting(
        Language language,
        Anomaly.Safe safe,
        List<EventParticipant> participants,
        int partySize,
        LaunchedEvent event
    ) {
        final var map = new HashMap<String, Object>();
        map.put("count", participants.size());
        map.put("party_size", partySize);
        map.put("duration", CommonLocalization.duration(language, TimeUtils.moscowTime(), event.endDate()));
        map.put("participants", participantsText(language, participants, Optional.of(safe.ownerPersonageId())));
        map.put("pve_format", pveFormat(language, safe.template()));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, AnomalyResource::anomalyPveWaiting),
            map
        );
    }

    public static String searching(
        Language language,
        List<EventParticipant> participants,
        int partySize,
        Duration maxDuration,
        PersonageId ownerPersonageId
    ) {
        final var map = new HashMap<String, Object>();
        map.put("count", participants.size());
        map.put("party_size", partySize);
        map.put("participants", participantsText(language, participants, Optional.of(ownerPersonageId)));
        map.put("max_duration", CommonLocalization.duration(language, maxDuration));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, AnomalyResource::anomalySearching),
            map
        );
    }

    public static String challenge(
        Language language,
        Anomaly anomaly,
        Group initiatorGroup,
        List<EventParticipant> participants,
        int partySize
    ) {
        final Optional<PersonageId> opponentOwner = switch (anomaly) {
            case Anomaly.Dangerous.Accepted accepted -> Optional.of(accepted.opponentOwnerPersonageId());
            case Anomaly.Dangerous.Challenged _ -> Optional.empty();
            default -> Optional.empty();
        };
        final var map = new HashMap<String, Object>();
        map.put("count", participants.size());
        map.put("party_size", partySize);
        map.put("participants", participantsText(language, participants, opponentOwner));
        map.put("initiator_group", LocaleUtils.groupNameWithBadge(initiatorGroup));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, AnomalyResource::anomalyChallenge),
            map
        );
    }

    public static String safeCompleted(Language language, AnomalyReward reward) {
        return rewardText(language, AnomalyResource::anomalySafeCompleted, reward, "");
    }

    public static String battleResult(
        Language language,
        Group winnerGroup,
        Group loserGroup,
        List<AnomalyPersonageResult> winnerResults,
        List<AnomalyPersonageResult> loserResults,
        AnomalyReward reward
    ) {
        final var params = new HashMap<String, Object>();
        params.put(
            "groups_top",
            battleGroupTop(language, winnerGroup, winnerResults, true).stripTrailing()
                + "\n\n"
                + battleGroupTop(language, loserGroup, loserResults, false).stripTrailing()
        );
        putRewardParams(params, reward);
        params.put("anomaly_report_command", CommandType.ANOMALY_REPORT.getText());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, AnomalyResource::anomalyBattleResult),
            params
        );
    }

    private static String battleGroupTop(
        Language language,
        Group group,
        List<AnomalyPersonageResult> results,
        boolean victory
    ) {
        final var sorted = new ArrayList<>(results);
        sorted.sort(Comparator.comparingLong(
            (AnomalyPersonageResult it) -> it.stats().damageDealtAndTaken()
        ).reversed());
        final var top = new StringBuilder();
        final int topCount = Math.min(5, sorted.size());
        for (int i = 0; i < topCount; ++i) {
            top.append(i + 1).append(". ").append(personageResult(language, sorted.get(i)));
            if (i < topCount - 1) {
                top.append("\n");
            }
        }
        final var params = new HashMap<String, Object>();
        params.put("result_icon", victory ? "🏆" : "💀");
        params.put("group_name_with_badge", LocaleUtils.groupNameWithBadge(group));
        params.put("participants_top", top.toString());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, AnomalyResource::anomalyBattleGroupTop),
            params
        );
    }

    public static String pveBattleResult(Language language, EventResult.AnomalyResult.PveBattleFinished result) {
        final var sorted = new ArrayList<>(result.personageResults());
        sorted.sort(Comparator.comparingLong((AnomalyPersonageResult it) -> it.stats().damageDealtAndTaken()).reversed());
        final var top = new StringBuilder();
        final int topCount = Math.min(5, sorted.size());
        for (int i = 0; i < topCount; ++i) {
            top.append(i + 1).append(". ").append(personageResult(language, sorted.get(i)));
            if (i < topCount - 1) {
                top.append("\n");
            }
        }

        long remainEnemiesHealth = 0;
        long totalEnemiesHealth = 0;
        long remainEnemies = 0;
        for (final var enemy : result.enemyStats()) {
            remainEnemiesHealth += enemy.remainHealth();
            totalEnemiesHealth += enemy.initialHealth();
            if (!enemy.isDead()) {
                ++remainEnemies;
            }
        }
        long participantsHealth = 0;
        long participantsMaxHealth = 0;
        long livingParticipants = 0;
        for (final var personageResult : result.personageResults()) {
            participantsHealth += personageResult.stats().remainHealth();
            participantsMaxHealth += personageResult.stats().initialHealth();
            if (!personageResult.stats().isDead()) {
                ++livingParticipants;
            }
        }

        final var params = new HashMap<String, Object>();
        params.put(
            "success_or_failure",
            result.victory()
                ? resources.getOrDefaultRandom(language, AnomalyResource::successPve)
                : resources.getOrDefaultRandom(language, AnomalyResource::failurePve)
        );
        params.put("remain_enemies_health", remainEnemiesHealth);
        params.put("total_enemies_health", totalEnemiesHealth);
        params.put("remain_enemies_count", remainEnemies);
        params.put("total_enemies_count", result.enemyStats().size());
        params.put("participants_health", participantsHealth);
        params.put("participants_max_health", participantsMaxHealth);
        params.put("living_participants", livingParticipants);
        params.put("total_participants", result.personageResults().size());
        params.put("top_participants_list", top.toString());
        putRewardParams(params, result.reward());
        params.put("anomaly_report_command", CommandType.ANOMALY_REPORT.getText());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, AnomalyResource::anomalyPveBattleResult),
            params
        );
    }

    public static String report(
        Language language,
        PersonageBattleResult result,
        LaunchedEvent event
    ) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, AnomalyResource::report),
            Map.of(
                "personage_battle_report",
                CommonLocalization.personageBattleReport(language, result, event, Optional.empty())
            )
        );
    }

    public static String reportNotPresentForPersonage(Language language) {
        return resources.getOrDefault(language, AnomalyResource::reportNotPresentForPersonage);
    }

    public static String lastGroupAnomalyReportNotFound(Language language) {
        return resources.getOrDefault(language, AnomalyResource::lastGroupAnomalyReportNotFound);
    }

    public static String shortGroupReport(
        Language language,
        PersonageBattleResult result,
        ru.homyakin.seeker.game.personage.models.Personage personage
    ) {
        return CommonLocalization.shortPersonageBattleReport(
            language,
            result,
            personage,
            Optional.<RaidItem>empty()
        );
    }

    public static String personageResult(Language language, AnomalyPersonageResult result) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, AnomalyResource::anomalyPersonageResult),
            Map.of(
                "dead_icon_or_empty", result.stats().isDead() ? Icons.DEAD : "",
                "personage_badge_with_name", LocaleUtils.personageNameWithBadge(result.personage()),
                "damage_dealt", result.stats().damageDealt(),
                "damage_taken", result.stats().damageTaken(),
                "money", result.reward().money().value(),
                "money_icon", Icons.MONEY,
                "storm_shards", stormShardsSuffix(result.reward())
            )
        );
    }

    public static String expired(Language language) {
        return resources.getOrDefault(language, AnomalyResource::anomalyExpired);
    }

    public static String safeModeButton(Language language) {
        return resources.getOrDefault(language, AnomalyResource::safeModeButton);
    }

    public static String dangerousModeButton(Language language) {
        return resources.getOrDefault(language, AnomalyResource::dangerousModeButton);
    }

    public static String joinButton(Language language) {
        return resources.getOrDefault(language, AnomalyResource::joinButton);
    }

    public static String readyButton(Language language) {
        return resources.getOrDefault(language, AnomalyResource::readyButton);
    }

    public static String backToOutpostButton(Language language) {
        return resources.getOrDefault(language, AnomalyResource::backToOutpostButton);
    }

    public static String successJoin(Language language) {
        return resources.getOrDefault(language, AnomalyResource::successJoin);
    }

    public static String successReadySafe(Language language) {
        return resources.getOrDefault(language, AnomalyResource::successReadySafe);
    }

    public static String successReadySearch(Language language) {
        return resources.getOrDefault(language, AnomalyResource::successReadySearch);
    }

    public static String successReadyBattle(Language language) {
        return resources.getOrDefault(language, AnomalyResource::successReadyBattle);
    }

    public static String error(Language language, AnomalyError error) {
        return switch (error) {
            case AnomalyError.NotRegistered _ ->
                resources.getOrDefault(language, AnomalyResource::errorNotRegistered);
            case AnomalyError.NoStormScanner _ ->
                resources.getOrDefault(language, AnomalyResource::errorNoStormScanner);
            case AnomalyError.AlreadyStartedToday _ ->
                resources.getOrDefault(language, AnomalyResource::errorAlreadyStartedToday);
            case AnomalyError.ActiveAnomalyExists _ ->
                resources.getOrDefault(language, AnomalyResource::errorActiveExists);
            case AnomalyError.NotGroupMember _ ->
                resources.getOrDefault(language, AnomalyResource::errorNotMember);
            case AnomalyError.NotOwner _ ->
                resources.getOrDefault(language, AnomalyResource::errorNotOwner);
            case AnomalyError.InvalidPhase _ ->
                resources.getOrDefault(language, AnomalyResource::errorInvalidPhase);
            case AnomalyError.PartyNotFull _ ->
                resources.getOrDefault(language, AnomalyResource::errorPartyNotFull);
            case AnomalyError.PartyEmpty _ ->
                resources.getOrDefault(language, AnomalyResource::errorPartyEmpty);
            case AnomalyError.RosterLocked _ ->
                resources.getOrDefault(language, AnomalyResource::errorRosterLocked);
            case AnomalyError.AlreadyJoined _ ->
                resources.getOrDefault(language, AnomalyResource::errorAlreadyJoined);
            case AnomalyError.AlreadyInAnomaly _ ->
                resources.getOrDefault(language, AnomalyResource::errorAlreadyInAnomaly);
            case AnomalyError.PartyFull _ ->
                resources.getOrDefault(language, AnomalyResource::errorPartyFull);
            case AnomalyError.EventLocked _ ->
                resources.getOrDefault(language, AnomalyResource::errorEventLocked);
            case AnomalyError.FinalStatus _ ->
                resources.getOrDefault(language, AnomalyResource::errorFinal);
            case AnomalyError.EventNotFound _ ->
                resources.getOrDefault(language, AnomalyResource::errorNotFound);
        };
    }

    private static String gathering(
        Language language,
        AnomalyMode mode,
        List<EventParticipant> participants,
        int partySize,
        LaunchedEvent event,
        Optional<PersonageId> ownerPersonageId,
        String pveFormat
    ) {
        final var map = new HashMap<String, Object>();
        map.put("mode", modeName(language, mode));
        map.put("count", participants.size());
        map.put("party_size", partySize);
        map.put("duration", CommonLocalization.duration(language, TimeUtils.moscowTime(), event.endDate()));
        map.put("participants", participantsText(language, participants, ownerPersonageId));
        map.put("pve_format", pveFormat);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, AnomalyResource::anomalyGathering),
            map
        );
    }

    private static String modeName(Language language, AnomalyMode mode) {
        return switch (mode) {
            case SAFE -> resources.getOrDefault(language, AnomalyResource::modeSafe);
            case DANGEROUS -> resources.getOrDefault(language, AnomalyResource::modeDangerous);
        };
    }

    private static String pveFormat(Language language, AnomalyPveTemplate template) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, AnomalyResource::anomalyPveFormat),
            Map.of(
                "attack", BattleLocalization.attackTypeName(language, template.attackType()),
                "defense", BattleLocalization.defenseTypeName(language, template.defenseType()),
                "formation", formationName(language, template.formation())
            )
        );
    }

    private static String formationName(Language language, AnomalyPveFormation formation) {
        return switch (formation) {
            case STRONG_BACK_LINE ->
                resources.getOrDefault(language, AnomalyResource::formationStrongBackLine);
            case STRONG_FRONT_LINE ->
                resources.getOrDefault(language, AnomalyResource::formationStrongFrontLine);
            case MID_LINE ->
                resources.getOrDefault(language, AnomalyResource::formationMidLine);
            case SPLIT_WINGS ->
                resources.getOrDefault(language, AnomalyResource::formationSplitWings);
        };
    }

    private static String participantsText(
        Language language,
        List<EventParticipant> participants,
        Optional<PersonageId> ownerPersonageId
    ) {
        if (participants.isEmpty()) {
            return "-";
        }
        return participants.stream()
            .map(it -> {
                final var isLeader = ownerPersonageId.filter(it.personage().id()::equals).isPresent();
                final var template = isLeader
                    ? resources.getOrDefault(language, AnomalyResource::participantLineLeader)
                    : resources.getOrDefault(language, AnomalyResource::participantLine);
                return StringNamedTemplate.format(
                    template,
                    Map.of("name", LocaleUtils.personageNameWithBadge(it.personage()))
                );
            })
            .collect(Collectors.joining("\n"));
    }

    private static String rewardText(
        Language language,
        java.util.function.Function<AnomalyResource, String> template,
        AnomalyReward reward,
        String battleLink
    ) {
        final var map = new HashMap<String, Object>();
        putRewardParams(map, reward);
        map.put("battle_link", battleLink == null ? "" : battleLink);
        return StringNamedTemplate.format(resources.getOrDefault(language, template), map);
    }

    private static void putRewardParams(Map<String, Object> params, AnomalyReward reward) {
        params.put("reward", reward.money().value());
        params.put("money_icon", Icons.MONEY);
        params.put("storm_shards_reward", stormShardsRewardText(reward));
    }

    private static String stormShardsRewardText(AnomalyReward reward) {
        if (reward.stormShards().isZero()) {
            return "";
        }
        return " +" + reward.stormShards().value() + Icons.STORM_SHARD;
    }

    private static String stormShardsSuffix(AnomalyReward reward) {
        if (reward.stormShards().isZero()) {
            return "";
        }
        return " +" + reward.stormShards().value() + Icons.STORM_SHARD;
    }
}
