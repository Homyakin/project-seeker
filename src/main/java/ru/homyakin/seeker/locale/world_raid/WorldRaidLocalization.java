package ru.homyakin.seeker.locale.world_raid;

import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaid;
import ru.homyakin.seeker.game.event.world_raid.entity.JoinWorldRaidError;
import ru.homyakin.seeker.game.event.world_raid.entity.battle.GroupWorldRaidBattleResult;
import ru.homyakin.seeker.game.event.world_raid.entity.battle.PersonageWorldRaidBattleResult;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageBattleResult;
import ru.homyakin.seeker.game.top.models.TopWorldRaidResearchResult;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.top.TopUtils;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class WorldRaidLocalization {
    private static final Resources<WorldRaidResource> resources = new Resources<>();

    public static void add(Language language, WorldRaidResource resource) {
        resources.add(language, resource);
    }

    public static String worldRaidResearchEnd(
        Language language,
        TopWorldRaidResearchResult result
    ) {
        final var params = new HashMap<String, Object>();
        final var topPersonageList = TopUtils.createTopList(language, result);
        params.put("top_personage_list", topPersonageList);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, WorldRaidResource::worldRaidResearchEnd),
            params
        );
    }

    public static String worldRaidBattle(
        Language language,
        ActiveWorldRaid worldRaid,
        LaunchedEvent event
    ) {
        return worldRaidBattle(language, worldRaid, event, List.of(), 0);
    }

    public static String worldRaidBattle(
        Language language,
        ActiveWorldRaid worldRaid,
        LaunchedEvent event,
        List<Group> groups,
        int participantsCount
    ) {
        final var params = new HashMap<String, Object>();
        params.put("world_raid_intro", worldRaid.getLocaleOrDefault(language).intro());
        params.put("enemies_health", worldRaid.info().health());
        params.put("enemies_count", 1);
        params.put("money_icon", Icons.MONEY);
        params.put("fund_value", worldRaid.fund().value());
        params.put("duration", CommonLocalization.duration(language, event.duration()));
        params.put("optional_participants", worldRaidParticipants(language, groups, participantsCount));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, WorldRaidResource::worldRaidBattle),
            params
        );
    }

    public static String endedWorldRaidBattle(
        Language language,
        ActiveWorldRaid worldRaid,
        List<Group> groups,
        int participantsCount
    ) {
        final var params = new HashMap<String, Object>();
        params.put("world_raid_intro", worldRaid.getLocaleOrDefault(language).intro());
        params.put("enemies_health", worldRaid.info().health());
        params.put("enemies_count", 1);
        params.put("money_icon", Icons.MONEY);
        params.put("fund_value", worldRaid.fund().value());
        params.put("optional_participants", worldRaidParticipants(language, groups, participantsCount));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, WorldRaidResource::endedWorldRaidBattle),
            params
        );
    }

    private static String worldRaidParticipants(Language language, List<Group> groups, int participantsCount) {
        if (participantsCount == 0) {
            return "";
        }
        final var params = new HashMap<String, Object>();
        final var tags = groups.stream().map(LocaleUtils::groupTagWithBadge).collect(Collectors.joining(", "));
        params.put("group_tags", tags);
        params.put("participants_count", participantsCount);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, WorldRaidResource::worldRaidParticipants),
            params
        );
    }

    public static String joinWorldRaidButton(Language language, int energy) {
        final var params = new HashMap<String, Object>();
        params.put("energy_icon", Icons.ENERGY);
        params.put("energy_cost", energy);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, WorldRaidResource::joinWorldRaidButton),
            params
        );
    }

    public static String groupNotification(Language language, String channel) {
        final var params = new HashMap<String, Object>();
        params.put("channel", channel);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, WorldRaidResource::groupNotification),
            params
        );
    }

    public static String successJoin(Language language) {
        return resources.getOrDefault(language, WorldRaidResource::successJoin);
    }

    public static String joinError(Language language, JoinWorldRaidError error) {
        return switch (error) {
            case JoinWorldRaidError.AlreadyJoined _ -> alreadyInRaid(language);
            case JoinWorldRaidError.NotEnoughEnergy _ -> notEnoughEnergy(language);
            case JoinWorldRaidError.NotFound _ -> raidNotFound(language);
            case JoinWorldRaidError.NotInRegisteredGroup _ -> mustBeInRegisteredGroup(language);
        };
    }

    private static String alreadyInRaid(Language language) {
        return resources.getOrDefault(language, WorldRaidResource::alreadyInRaid);
    }

    private static String notEnoughEnergy(Language language) {
        return resources.getOrDefault(language, WorldRaidResource::notEnoughEnergy);
    }

    private static String mustBeInRegisteredGroup(Language language) {
        return resources.getOrDefault(language, WorldRaidResource::mustBeInRegisteredGroup);
    }

    private static String raidNotFound(Language language) {
        return resources.getOrDefault(language, WorldRaidResource::raidNotFound);
    }

    public static String raidBattleResult(Language language, EventResult.WorldRaidBattleResult result) {
        if (result.isWin()) {
            return successBattle(language, result);
        } else {
            return failedBattle(language, result);
        }
    }

    private static String successBattle(Language language, EventResult.WorldRaidBattleResult result) {
        final var params = new HashMap<String, Object>();
        params.put("raid_report_command", CommandType.WORLD_RAID_REPORT.getText());
        params.put("battle_result", battleResult(language, result));
        final var rarityCountMap = new HashMap<ItemRarity, Integer>();
        for (final var personageResult : result.personageResults()) {
            if (personageResult.generatedItem().isEmpty()) {
                continue;
            }
            rarityCountMap.merge(personageResult.generatedItem().get().rarity(), 1, Integer::sum);
        }
        final var rarityCounts = rarityCountMap.entrySet().stream()
            .sorted(Comparator.comparingInt(e -> e.getKey().id))
            .map(entry -> rarityCount(language, entry.getKey(), entry.getValue()))
            .toList();
        params.put("rarity_counts", String.join("\n", rarityCounts));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, WorldRaidResource::successBattle),
            params
        );
    }

    private static String rarityCount(Language language, ItemRarity rarity, int count) {
        final var params = new HashMap<String, Object>();
        params.put("rarity_icon", rarity.icon);
        params.put("count", count);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, WorldRaidResource::rarityCount),
            params
        );
    }

    private static String failedBattle(Language language, EventResult.WorldRaidBattleResult result) {
        final var params = new HashMap<String, Object>();
        params.put("raid_report_command", CommandType.WORLD_RAID_REPORT.getText());
        params.put("battle_result", battleResult(language, result));
        params.put("enemies_health", result.remainedInfo().health());
        params.put("enemies_count", 1);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, WorldRaidResource::failedBattle),
            params
        );
    }

    private static String battleResult(Language language, EventResult.WorldRaidBattleResult result) {
        final var topGroups = result.groupResults().stream()
            .sorted(groupComparator)
            .limit(5)
            .toList();
        final var groupResults = new StringBuilder();
        for (int i = 0; i < topGroups.size(); i++) {
            groupResults
                .append(i + 1)
                .append(". ")
                .append(groupResult(language, topGroups.get(i)));
            if (i < topGroups.size() - 1) {
                groupResults.append("\n");
            }
        }
        final var topPersonages = result.personageResults().stream()
            .sorted(personageComparator)
            .limit(5)
            .toList();
        final var personageResults = new StringBuilder();
        for (int i = 0; i < topPersonages.size(); i++) {
            personageResults
                .append(i + 1)
                .append(". ")
                .append(personageResult(language, topPersonages.get(i)));
            if (i < topPersonages.size() - 1) {
                personageResults.append("\n");
            }
        }
        final var params = new HashMap<String, Object>();
        params.put("group_results", groupResults);
        params.put("personage_results", personageResults.toString());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, WorldRaidResource::battleResult),
            params
        );
    }

    private static String groupResult(Language language, GroupWorldRaidBattleResult result) {
        final var params = new HashMap<String, Object>();
        params.put("group_tag_with_badage", LocaleUtils.groupTagWithBadge(result.group()));
        params.put("damage_dealt", result.stats().damageDealt());
        params.put("damage_taken", result.stats().damageTaken());
        params.put("money_icon", Icons.MONEY);
        params.put("money", result.reward().value());
        params.put("participants_count", result.stats().totalPersonages());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, WorldRaidResource::groupResult),
            params
        );
    }

    private static String personageResult(Language language, PersonageWorldRaidBattleResult result) {
        final var params = new HashMap<String, Object>();
        params.put("personage_badge_with_name", LocaleUtils.personageNameWithBadge(result.personage()));
        params.put("damage_dealt", result.stats().damageDealt());
        params.put("damage_taken", result.stats().damageTaken());
        params.put("money_icon", Icons.MONEY);
        params.put("money", result.reward().value());
        params.put("dead_icon_or_empty", result.stats().isDead() ? Icons.DEAD : "");
        return StringNamedTemplate.format(
            resources.getOrDefault(language, WorldRaidResource::personageResult),
            params
        );
    }

    public static String personageWorldRaidReportNotFound(Language language) {
        return resources.getOrDefault(language, WorldRaidResource::personageWorldRaidReportNotFound);
    }

    public static String personageWorldRaidReport(
        Language language,
        PersonageBattleResult result,
        LaunchedEvent event,
        Optional<Item> item
    ) {
        final var params = new HashMap<String, Object>();
        params.put("personage_battle_report", CommonLocalization.personageBattleReport(language, result, event, item));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, WorldRaidResource::personageWorldRaidReport),
            params
        );
    }

    public static String groupWorldRaidReportNotFound(Language language) {
        return resources.getOrDefault(language, WorldRaidResource::groupWorldRaidReportNotFound);
    }

    private static final Comparator<GroupWorldRaidBattleResult> groupComparator =
        Comparator.<GroupWorldRaidBattleResult>comparingLong(
            result -> result.stats().damageDealtAndTaken()
        ).reversed();
    private static final Comparator<PersonageWorldRaidBattleResult> personageComparator =
        Comparator.<PersonageWorldRaidBattleResult>comparingLong(
            result -> result.stats().damageDealtAndTaken()
        ).reversed();
}
