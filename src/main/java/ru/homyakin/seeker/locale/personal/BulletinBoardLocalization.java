package ru.homyakin.seeker.locale.personal;

import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestRequirements;
import ru.homyakin.seeker.game.event.personal_quest.model.StartedQuest;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaidState;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidResearchDonateError;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidInfo;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

import java.util.Collections;
import java.util.HashMap;

public class BulletinBoardLocalization {
    private static final Resources<BulletinBoardResource> resources = new Resources<>();

    public static void add(Language language, BulletinBoardResource resource) {
        resources.add(language, resource);
    }

    public static String bulletinBoard(Language language, PersonalQuestRequirements requirements) {
        final var params = new HashMap<String, Object>();
        params.put("quest_required_energy", requirements.requiredEnergy());
        params.put("energy_icon", Icons.ENERGY);
        params.put("time_icon", Icons.TIME);
        params.put("quest_duration", CommonLocalization.duration(language, requirements.requiredTime()));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, BulletinBoardResource::bulletinBoard),
            params
        );
    }

    public static String notEnoughEnergy(Language language, int requiredEnergy) {
        final var params = new HashMap<String, Object>();
        params.put("required_energy", requiredEnergy);
        params.put("energy_icon", Icons.ENERGY);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, BulletinBoardResource::notEnoughEnergy),
            params
        );
    }

    public static String personageInAnotherEvent(Language language) {
        return resources.getOrDefault(language, BulletinBoardResource::personageInAnotherEvent);
    }

    public static String startedQuest(Language language, StartedQuest startedQuest) {
        final var params = new HashMap<String, Object>();
        params.put("quest_intro", startedQuest.quest().getLocaleOrDefault(language).intro());
        params.put("time_icon", Icons.TIME);
        params.put("duration", CommonLocalization.duration(language, startedQuest.duration()));
        params.put("energy_icon", Icons.ENERGY);
        params.put("energy", startedQuest.takenEnergy());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, BulletinBoardResource::startedQuest),
            params
        );
    }

    public static String autoStartedQuest(Language language, StartedQuest startedQuest) {
        final var params = new HashMap<String, Object>();
        params.put("energy_recovered", CommonLocalization.energyRecovered(language));
        params.put("started_quest", startedQuest(language, startedQuest));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, BulletinBoardResource::autoStartedQuest),
            params
        );
    }

    public static String failedQuest(Language language, EventResult.PersonalQuestResult.Failure result) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, BulletinBoardResource::failedQuest),
            Collections.singletonMap("quest_failure", result.quest().getLocaleOrDefault(language).failure())
        );
    }

    public static String successQuest(Language language, EventResult.PersonalQuestResult.Success result) {
        final var params = new HashMap<String, Object>();
        params.put("quest_success", result.quest().getLocaleOrDefault(language).success());
        params.put("money_icon", Icons.MONEY);
        params.put("money_value", result.reward().value());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, BulletinBoardResource::successQuest),
            params
        );
    }

    public static String worldRaidInfo(Language language, WorldRaidInfo info, String channel) {
        return switch (info.worldRaid().state()) {
            case ActiveWorldRaidState.Research research -> worldRaidResearch(language, info, research);
            case ActiveWorldRaidState.Battle _ -> worldRaidBattle(language, info, channel);
        };
    }

    private static String worldRaidResearch(
        Language language,
        WorldRaidInfo info,
        ActiveWorldRaidState.Research research
    ) {
        final var params = new HashMap<String, Object>();
        params.put("donate_value", info.requiredForDonate().value());
        params.put("fund_value", info.worldRaid().fund().value());
        params.put("money_icon", Icons.MONEY);
        params.put("progress", "%.2f".formatted(research.progressInPercent()));
        params.put("donate_world_raid_command", CommandType.WORLD_RAID_DONATE.getText());
        params.put("world_raid_research_top_command", CommandType.WORLD_RAID_RESEARCH_TOP.getText());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, BulletinBoardResource::worldRaidResearch),
            params
        );
    }

    private static String worldRaidBattle(Language language, WorldRaidInfo info, String channel) {
        final var params = new HashMap<String, Object>();
        params.put("fund_value", info.worldRaid().fund().value());
        params.put("money_icon", Icons.MONEY);
        params.put("world_raid_channel", channel);
        params.put("world_raid_research_top_command", CommandType.WORLD_RAID_RESEARCH_TOP.getText());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, BulletinBoardResource::worldRaidBattle),
            params
        );
    }

    public static String worldRaidDonateError(Language language, WorldRaidResearchDonateError error) {
        return switch (error) {
            case WorldRaidResearchDonateError.ResearchCompleted _ ->
                resources.getOrDefault(language, BulletinBoardResource::worldRaidResearchIsCompleted);
            case WorldRaidResearchDonateError.NotEnoughMoney notEnoughMoney ->
                notEnoughMoneyForDonate(language, notEnoughMoney);
        };
    }

    private static String notEnoughMoneyForDonate(
        Language language,
        WorldRaidResearchDonateError.NotEnoughMoney error
    ) {
        final var params = new HashMap<String, Object>();
        params.put("required_value", error.required().value());
        params.put("money_icon", Icons.MONEY);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, BulletinBoardResource::notEnoughMoneyForDonate),
            params
        );
    }

    public static String successWorldRaidDonate(Language language, Money moneyToFund) {
        final var params = new HashMap<String, Object>();
        params.put("money_icon", Icons.MONEY);
        params.put("money_value", moneyToFund.value());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, BulletinBoardResource::successWorldRaidDonate),
            params
        );
    }
}
