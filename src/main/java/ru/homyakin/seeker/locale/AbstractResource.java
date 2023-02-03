package ru.homyakin.seeker.locale;

import java.util.HashMap;
import java.util.ListResourceBundle;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public abstract class AbstractResource extends ListResourceBundle {
    public static final String BASE_NAME = "ru.homyakin.seeker.locale.resource";

    public String welcomeGroup() {
        return getString(LocalizationKeys.WELCOME_GROUP.name());
    }

    public String welcomeUser() {
        return getString(LocalizationKeys.WELCOME_USER.name());
    }

    public String chooseLanguage() {
        return getString(LocalizationKeys.CHOOSE_LANGUAGE.name());
    }

    public String onlyAdminAction() {
        return getString(LocalizationKeys.ONLY_ADMIN_ACTION.name());
    }

    public String internalError() {
        return getString(LocalizationKeys.INTERNAL_ERROR.name());
    }

    public String joinRaidEvent() {
        return getString(LocalizationKeys.JOIN_RAID_EVENT.name());
    }

    public String raidStartsPrefix() {
        return getString(LocalizationKeys.RAID_STARTS_PREFIX.name());
    }

    public String hoursShort() {
        return getString(LocalizationKeys.HOURS_SHORT.name());
    }

    public String minutesShort() {
        return getString(LocalizationKeys.MINUTES_SHORT.name());
    }

    public String successJoinEvent() {
        return getString(LocalizationKeys.SUCCESS_JOIN_EVENT.name());
    }

    public String userAlreadyInThisEvent() {
        return getString(LocalizationKeys.USER_ALREADY_IN_THIS_EVENT.name());
    }

    public String userAlreadyInOtherEvent() {
        return getString(LocalizationKeys.USER_ALREADY_IN_OTHER_EVENT.name());
    }

    public String expiredEvent() {
        return getString(LocalizationKeys.EXPIRED_EVENT.name());
    }

    public String profile(Personage personage) {
        final var params = new HashMap<String, Object>() {{
            put("profile_icon", TextConstants.PROFILE_ICON);
            put("level_icon", TextConstants.LEVEL_ICON);
            put("exp_icon", TextConstants.EXP_ICON);
            put("personage_name", personage.name());
        }};
        return StringNamedTemplate.format(
            getString(LocalizationKeys.PROFILE_TEMPLATE.name()),
            params
        );
    }

    public String successRaid() {
        return getString(LocalizationKeys.SUCCESS_RAID.name());
    }

    public String failureRaid() {
        return getString(LocalizationKeys.FAILURE_RAID.name());
    }


    public String help() {
        return getString(LocalizationKeys.HELP.name());
    }

    public String changeNameWithoutName() {
        return getString(LocalizationKeys.CHANGE_NAME_WITHOUT_NAME.name());
    }

    public String personageNameInvalidLength(int minNameLength, int maxNameLength) {
        final var params = new HashMap<String, Object>() {{
            put("max_name_length", maxNameLength);
            put("min_name_length", minNameLength);
        }};
        return StringNamedTemplate.format(
            getString(LocalizationKeys.PERSONAGE_NAME_INVALID_LENGTH.name()),
            params
        );
    }

    public String personageNameInvalidSymbols() {
        return getString(LocalizationKeys.PERSONAGE_NAME_INVALID_SYMBOLS.name());
    }

    public String successNameChange() {
        return getString(LocalizationKeys.SUCCESS_NAME_CHANGE.name());
    }

    public String profileLevelUp() {
        return getString(LocalizationKeys.PROFILE_LEVEL_UP.name());
    }

    public String notEnoughLevelingPoints() {
        return getString(LocalizationKeys.NOT_ENOUGH_LEVELING_POINTS.name());
    }

    public String chooseLevelUpCharacteristic() {
        return getString(LocalizationKeys.CHOOSE_LEVEL_UP_CHARACTERISTIC.name());
    }

    public String successLevelUp() {
        return getString(LocalizationKeys.SUCCESS_LEVEL_UP.name());
    }

    public String profileButton() {
        return getString(LocalizationKeys.PROFILE_BUTTON.name());
    }

    public String languageButton() {
        return getString(LocalizationKeys.LANGUAGE_BUTTON.name());
    }

    public String duelMustBeReply() {
        return getString(LocalizationKeys.DUEL_MUST_BE_REPLY.name());
    }

    public String duelReplyMustBeToUser() {
        return getString(LocalizationKeys.DUEL_REPLY_MUST_BE_TO_USER.name());
    }

    public String duelWithYourself() {
        return getString(LocalizationKeys.DUEL_WITH_YOURSELF.name());
    }

    public String duelWithInitiatorLowHealth() {
        return getString(LocalizationKeys.DUEL_WITH_INITIATOR_LOW_HEALTH.name());
    }

    public String duelWithAcceptorLowHealth() {
        return getString(LocalizationKeys.DUEL_WITH_ACCEPTOR_LOW_HEALTH.name());
    }

    public String personageAlreadyStartDuel() {
        return getString(LocalizationKeys.PERSONAGE_ALREADY_START_DUEL.name());
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
            getString(LocalizationKeys.INIT_DUEL.name()),
            params
        );
    }

    public String notDuelAcceptingPersonage() {
        return getString(LocalizationKeys.NOT_DUEL_ACCEPTING_PERSONAGE.name());
    }

    public String expiredDuel() {
        return getString(LocalizationKeys.EXPIRED_DUEL.name());
    }

    public String declinedDuel() {
        return getString(LocalizationKeys.DECLINED_DUEL.name());
    }

    public String finishedDuel(Personage winnerPersonage, Personage looserPersonage) {
        final var params = new HashMap<String, Object>() {{
            put("level_icon", TextConstants.LEVEL_ICON);
            put("winner_personage_name", winnerPersonage.name());
            put("looser_personage_name", looserPersonage.name());
        }};
        return StringNamedTemplate.format(
            getString(LocalizationKeys.FINISHED_DUEL.name()),
            params
        );
    }

    public String acceptDuelButton() {
        return getString(LocalizationKeys.ACCEPT_DUEL_BUTTON.name());
    }

    public String declineDuelButton() {
        return getString(LocalizationKeys.DECLINE_DUEL_BUTTON.name());
    }
}
