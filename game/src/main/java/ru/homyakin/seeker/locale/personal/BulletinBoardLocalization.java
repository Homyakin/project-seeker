package ru.homyakin.seeker.locale.personal;

import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuest;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestRequirements;
import ru.homyakin.seeker.game.event.personal_quest.model.PersonalQuestResult;
import ru.homyakin.seeker.game.event.personal_quest.model.StartedQuest;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaidState;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidResearchDonateError;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidInfo;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

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
        params.put("take_quest_command", CommandType.TAKE_PERSONAL_QUEST_COMMAND.getText());
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
        params.put("quest_intro", questIntro(language, startedQuest));
        params.put("time_icon", Icons.TIME);
        params.put("duration", CommonLocalization.duration(language, startedQuest.duration()));
        params.put("energy_icon", Icons.ENERGY);
        params.put("energy", startedQuest.takenEnergy());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, BulletinBoardResource::startedQuest),
            params
        );
    }

    private static String questIntro(Language language, StartedQuest startedQuest) {
        return switch (startedQuest) {
            case StartedQuest.Single single -> single.quest().getLocaleOrDefault(language).intro();
            case StartedQuest.Multiple _ ->
                resources.getOrDefaultRandom(language, BulletinBoardResource::multipleQuestsIntro);
        };
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

    public static String personalQuestResult(Language language, EventResult.PersonalQuestEventResult result) {
        return switch (result) {
            case EventResult.PersonalQuestEventResult.Single single -> singleQuestResult(language, single);
            case EventResult.PersonalQuestEventResult.Multiple multiple -> multipleQuestsFinished(language, multiple);
        };
    }

    private static String singleQuestResult(Language language, EventResult.PersonalQuestEventResult.Single result) {
        return switch (result.result()) {
            case PersonalQuestResult.Success success -> successSingleQuest(
                language,
                result.quest(),
                result.personage(),
                success.reward()
            );
            case PersonalQuestResult.Failure _ -> failedSingleQuest(language, result.quest(), result.personage());
        };
    }

    private static String failedSingleQuest(
        Language language,
        PersonalQuest quest,
        Personage personage
    ) {
        final var params = new HashMap<String, Object>();
        params.put("quest_failure", quest.getLocaleOrDefault(language).failure());
        params.put("energy_icon", Icons.ENERGY);
        params.put("current_energy", personage.energy().value());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, BulletinBoardResource::failedSingleQuest),
            params
        );
    }

    private static String successSingleQuest(
        Language language,
        PersonalQuest quest,
        Personage personage,
        Money reward
    ) {
        final var params = new HashMap<String, Object>();
        params.put("quest_success", quest.getLocaleOrDefault(language).success());
        params.put("money_icon", Icons.MONEY);
        params.put("money_value", reward.value());
        params.put("current_energy", personage.energy().value());
        params.put("energy_icon", Icons.ENERGY);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, BulletinBoardResource::successSingleQuest),
            params
        );
    }

    public static String incorrectQuestCount(Language language) {
        return resources.getOrDefault(language, BulletinBoardResource::incorrectQuestCount);
    }

    private static String multipleQuestsFinished(
        Language language,
        EventResult.PersonalQuestEventResult.Multiple result
    ) {
        final var params = new HashMap<String, Object>();
        params.put("multiple_quests_outro", multipleQuestsOutro(language));
        params.put("money_icon", Icons.MONEY);
        int reward = 0;
        final var results = new StringBuilder();
        for (final var questResult : result.results()) {
            switch (questResult) {
                case PersonalQuestResult.Success success -> {
                    reward += success.reward().value();
                    results.append(Icons.SUCCESS_QUEST);
                }
                case PersonalQuestResult.Failure _ -> results.append(Icons.FAILED_QUEST);
            }
        }
        params.put("quests_results", results.toString());
        params.put("money_value", reward);
        params.put("current_energy", result.personage().energy().value());
        params.put("energy_icon", Icons.ENERGY);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, BulletinBoardResource::multipleQuestsFinished),
            params
        );
    }

    private static String multipleQuestsIntro(Language language) {
        return resources.getOrDefaultRandom(language, BulletinBoardResource::multipleQuestsIntro);
    }

    private static String multipleQuestsOutro(Language language) {
        return resources.getOrDefaultRandom(language, BulletinBoardResource::multipleQuestsOutro);
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
