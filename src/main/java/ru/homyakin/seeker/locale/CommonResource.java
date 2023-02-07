package ru.homyakin.seeker.locale;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class CommonResource {
    private final Map<String, Object> localeMap;

    public CommonResource(Map<String, Object> localeMap) {
        this.localeMap = localeMap;
    }

    public String welcomeGroup() {
        return getString(LocalizationKeys.WELCOME_GROUP.tomlKey());
    }

    public String welcomeUser() {
        return getString(LocalizationKeys.WELCOME_USER.tomlKey());
    }

    public String chooseLanguage() {
        return getString(LocalizationKeys.CHOOSE_LANGUAGE.tomlKey());
    }

    public String onlyAdminAction() {
        return getString(LocalizationKeys.ONLY_ADMIN_ACTION.tomlKey());
    }

    public String internalError() {
        return getString(LocalizationKeys.INTERNAL_ERROR.tomlKey());
    }

    public String joinRaidEvent() {
        return getString(LocalizationKeys.JOIN_RAID_EVENT.tomlKey());
    }

    public String raidStartsPrefix() {
        return getString(LocalizationKeys.RAID_STARTS_PREFIX.tomlKey());
    }

    public String hoursShort() {
        return getString(LocalizationKeys.HOURS_SHORT.tomlKey());
    }

    public String minutesShort() {
        return getString(LocalizationKeys.MINUTES_SHORT.tomlKey());
    }

    public String successJoinEvent() {
        return getString(LocalizationKeys.SUCCESS_JOIN_EVENT.tomlKey());
    }

    public String userAlreadyInThisEvent() {
        return getString(LocalizationKeys.USER_ALREADY_IN_THIS_EVENT.tomlKey());
    }

    public String userAlreadyInOtherEvent() {
        return getString(LocalizationKeys.USER_ALREADY_IN_OTHER_EVENT.tomlKey());
    }

    public String expiredEvent() {
        return getString(LocalizationKeys.EXPIRED_EVENT.tomlKey());
    }

    public String profile(Personage personage) {
        final var params = new HashMap<String, Object>() {{
            put("profile_icon", TextConstants.PROFILE_ICON);
            put("level_icon", TextConstants.LEVEL_ICON);
            put("exp_icon", TextConstants.EXP_ICON);
            put("personage_name", personage.name());
        }};
        return StringNamedTemplate.format(
            getString(LocalizationKeys.PROFILE_TEMPLATE.tomlKey()),
            params
        );
    }

    public String successRaid() {
        return getString(LocalizationKeys.SUCCESS_RAID.tomlKey());
    }

    public String failureRaid() {
        return getString(LocalizationKeys.FAILURE_RAID.tomlKey());
    }

    public String help() {
        return StringNamedTemplate.format(
            getString(LocalizationKeys.HELP.tomlKey()),
            new HashMap<>() {{
                put("news_channel_username", TextConstants.TELEGRAM_CHANNEL_USERNAME);
                put("language_command", CommandType.CHANGE_LANGUAGE.getText());
                put("profile_command", CommandType.GET_PROFILE.getText());
                put("help_command", CommandType.HELP.getText());
                put("duel_command", CommandType.START_DUEL.getText());
                put("name_command", CommandType.CHANGE_NAME.getText());
                put("github_link", TextConstants.SOURCE_LINK);
            }}
        );
    }

    public String changeNameWithoutName() {
        return StringNamedTemplate.format(
            getString(LocalizationKeys.CHANGE_NAME_WITHOUT_NAME.tomlKey()),
            Collections.singletonMap("name_command", CommandType.CHANGE_NAME.getText())
        );
    }

    public String personageNameInvalidLength(int minNameLength, int maxNameLength) {
        final var params = new HashMap<String, Object>() {{
            put("max_name_length", maxNameLength);
            put("min_name_length", minNameLength);
        }};
        return StringNamedTemplate.format(
            getString(LocalizationKeys.PERSONAGE_NAME_INVALID_LENGTH.tomlKey()),
            params
        );
    }

    public String personageNameInvalidSymbols() {
        return getString(LocalizationKeys.PERSONAGE_NAME_INVALID_SYMBOLS.tomlKey());
    }

    public String successNameChange() {
        return getString(LocalizationKeys.SUCCESS_NAME_CHANGE.tomlKey());
    }

    public String profileLevelUp() {
        return StringNamedTemplate.format(
            getString(LocalizationKeys.PROFILE_LEVEL_UP.tomlKey()),
            Collections.singletonMap("level_up_command", CommandType.LEVEL_UP.getText())
        );
    }

    public String notEnoughLevelingPoints() {
        return getString(LocalizationKeys.NOT_ENOUGH_LEVELING_POINTS.tomlKey());
    }

    public String chooseLevelUpCharacteristic() {
        return getString(LocalizationKeys.CHOOSE_LEVEL_UP_CHARACTERISTIC.tomlKey());
    }

    public String successLevelUp() {
        return getString(LocalizationKeys.SUCCESS_LEVEL_UP.tomlKey());
    }

    public String profileButton() {
        return getString(LocalizationKeys.PROFILE_BUTTON.tomlKey());
    }

    public String languageButton() {
        return getString(LocalizationKeys.LANGUAGE_BUTTON.tomlKey());
    }

    public String duelMustBeReply() {
        return getString(LocalizationKeys.DUEL_MUST_BE_REPLY.tomlKey());
    }

    public String duelReplyMustBeToUser() {
        return getString(LocalizationKeys.DUEL_REPLY_MUST_BE_TO_USER.tomlKey());
    }

    public String duelWithYourself() {
        return getString(LocalizationKeys.DUEL_WITH_YOURSELF.tomlKey());
    }

    public String duelWithInitiatorLowHealth() {
        return getString(LocalizationKeys.DUEL_WITH_INITIATOR_LOW_HEALTH.tomlKey());
    }

    public String duelWithAcceptorLowHealth() {
        return getString(LocalizationKeys.DUEL_WITH_ACCEPTOR_LOW_HEALTH.tomlKey());
    }

    public String personageAlreadyStartDuel() {
        return getString(LocalizationKeys.PERSONAGE_ALREADY_START_DUEL.tomlKey());
    }

    public String initDuel(Personage initiatingPersonage, Personage acceptingPersonage) {
        final var params = new HashMap<String, Object>() {{
            put("level_icon", TextConstants.LEVEL_ICON);
            put("health_icon", TextConstants.HEALTH_ICON);
            put("initiating_personage_name", initiatingPersonage.name());
            put("initiating_personage_health", initiatingPersonage.health());
            put("accepting_personage_name", acceptingPersonage.name());
            put("accepting_personage_health", acceptingPersonage.health());
        }};
        return StringNamedTemplate.format(
            getString(LocalizationKeys.INIT_DUEL.tomlKey()),
            params
        );
    }

    public String notDuelAcceptingPersonage() {
        return getString(LocalizationKeys.NOT_DUEL_ACCEPTING_PERSONAGE.tomlKey());
    }

    public String expiredDuel() {
        return getString(LocalizationKeys.EXPIRED_DUEL.tomlKey());
    }

    public String declinedDuel() {
        return getString(LocalizationKeys.DECLINED_DUEL.tomlKey());
    }

    public String finishedDuel(Personage winnerPersonage, Personage looserPersonage) {
        final var params = new HashMap<String, Object>() {{
            put("level_icon", TextConstants.LEVEL_ICON);
            put("winner_personage_name", winnerPersonage.name());
            put("looser_personage_name", looserPersonage.name());
        }};
        return StringNamedTemplate.format(
            getString(LocalizationKeys.FINISHED_DUEL.tomlKey()),
            params
        );
    }

    public String acceptDuelButton() {
        return getString(LocalizationKeys.ACCEPT_DUEL_BUTTON.tomlKey());
    }

    public String declineDuelButton() {
        return getString(LocalizationKeys.DECLINE_DUEL_BUTTON.tomlKey());
    }

    private String getString(String key) {
        return (String) localeMap.get(key);
    }
}
