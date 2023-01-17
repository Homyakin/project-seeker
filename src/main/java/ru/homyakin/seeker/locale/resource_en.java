package ru.homyakin.seeker.locale;

import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.CommandType;

public class resource_en extends AbstractResource {
    private static final Object[][] contents =
        {
            {
                LocalizationKeys.WELCOME_GROUP.name(),
                "Greetings, Seekers."
            },
            {
                LocalizationKeys.WELCOME_USER.name(),
                "Greetings, Seeker."
            },
            {
                LocalizationKeys.CHOOSE_LANGUAGE.name(),
                "Choose language:"
            },
            {
                LocalizationKeys.ONLY_ADMIN_ACTION.name(),
                "This action is available only for administrators"
            },
            {
                LocalizationKeys.INTERNAL_ERROR.name(),
                "Unexpected error, try again later"
            },
            {
                LocalizationKeys.JOIN_BOSS_EVENT.name(),
                "Join raid" + TextConstants.RAID_ICON
            },
            {
                LocalizationKeys.BOSS_BATTLE_STARTS.name(),
                "The battle will begin in"
            },
            {
                LocalizationKeys.HOURS_SHORT.name(),
                "h"
            },
            {
                LocalizationKeys.MINUTES_SHORT.name(),
                "m"
            },
            {
                LocalizationKeys.SUCCESS_JOIN_EVENT.name(),
                "You have successfully joined the event!"
            },
            {
                LocalizationKeys.USER_ALREADY_IN_THIS_EVENT.name(),
                "You are already participating in this event!"
            },
            {
                LocalizationKeys.USER_ALREADY_IN_OTHER_EVENT.name(),
                "You are already participating in another event!"
            },
            {
                LocalizationKeys.EXPIRED_EVENT.name(),
                "The event is over!"
            },
            {
                LocalizationKeys.PROFILE_TEMPLATE.name(),
                """
                %s%s
                %sLevel: %s
                %sExperience: %s
                """.formatted(
                    TextConstants.PROFILE_ICON,
                    "%s",
                    TextConstants.LEVEL_ICON,
                    "%d",
                    TextConstants.EXP_ICON,
                    "%d/%d"
                )
            },
            {
                LocalizationKeys.SUCCESS_BOSS.name(),
                "Boss was defeated."
            },
            {
                LocalizationKeys.FAILURE_BOSS.name(),
                "Boss was stronger than the seekers."
            },
            {
                LocalizationKeys.TOP_PERSONAGES_BY_EXP_IN_GROUP.name(),
                TextConstants.EXP_ICON + "Top group personages by experience:%n%s"
            },
            {
                LocalizationKeys.HELP.name(),
                """
                Social RPG in Telegram!
                Just add to the group and join events.
                Official news channel - %s.
                
                Available commands (pm and chant):
                %s - change language;
                %s - show profile;
                %s - this message
                
                Only for group:
                %s - show top by experience in group
                
                Only for pm:
                %s - change name
                
                The source code of the game is <a href="%s">here</a>
                """.formatted(
                    TextConstants.TELEGRAM_CHANNEL_USERNAME,
                    CommandType.CHANGE_LANGUAGE.getText(),
                    CommandType.GET_PROFILE.getText(),
                    CommandType.HELP.getText(),
                    CommandType.TOP.getText(),
                    CommandType.CHANGE_NAME.getText(),
                    TextConstants.SOURCE_LINK
                )
            },
            {
                LocalizationKeys.CHANGE_NAME_WITHOUT_NAME.name(),
                "Enter the name separated by a space after the command: \"/name Name\""
            },
            {
                LocalizationKeys.NAME_TOO_LONG.name(),
                "The name must not exceed %d characters"
            },
            {
                LocalizationKeys.SUCCESS_NAME_CHANGE.name(),
                "Name has been successfully changed!"
            },
            {
                LocalizationKeys.PROFILE_LEVEL_UP.name(),
                "There are unspent leveling points! Press /level_up"
            },
            {
                LocalizationKeys.NOT_ENOUGH_LEVELING_POINTS.name(),
                "Not enough leveling points. Get leveled up and come back later."
            },
            {
                LocalizationKeys.CHOOSE_LEVEL_UP_CHARACTERISTIC.name(),
                "Choose characteristic to level up:white_check_mark:"
            },
            {
                LocalizationKeys.SUCCESS_LEVEL_UP.name(),
                "Success level up:white_check_mark:"
            },
            {
                LocalizationKeys.PROFILE_BUTTON.name(),
                TextConstants.PROFILE_ICON + "Profile"
            },
            {
                LocalizationKeys.LANGUAGE_BUTTON.name(),
                TextConstants.LANGUAGE_ICON + "Language"
            },
            {
                LocalizationKeys.DUEL_MUST_BE_REPLY.name(),
                "The duel should be a reply to another user's message"
            },
            {
                LocalizationKeys.DUEL_REPLY_MUST_BE_TO_USER.name(),
                "Your opponent must be a user"
            },
            {
                LocalizationKeys.DUEL_WITH_YOURSELF.name(),
                "You can't have a duel with yourself!"
            },
            {
                LocalizationKeys.PERSONAGE_ALREADY_START_DUEL.name(),
                "You have already started another duel! Wait for it to finish."
            },
            {
                LocalizationKeys.INIT_DUEL.name(),
                "The seeker " + TextConstants.LEVEL_ICON + "%d %s challenges the seeker " +
                    TextConstants.LEVEL_ICON + "%d %s to a duel.\n\n" +
                    "What will be his answer?"
            },
            {
                LocalizationKeys.NOT_DUEL_ACCEPTING_PERSONAGE.name(),
                "It wasn't you who was challenged to a duel!"
            },
            {
                LocalizationKeys.EXPIRED_DUEL.name(),
                "The challenge to the duel was ignored"
            },
            {
                LocalizationKeys.DECLINED_DUEL.name(),
                "The accepting party rejected the call!"
            },
            {
                LocalizationKeys.FINISHED_DUEL.name(),
                "The seeker " + TextConstants.LEVEL_ICON + "%d %s got the better of " +
                    TextConstants.LEVEL_ICON + "%d %s"
            },
            {
                LocalizationKeys.ACCEPT_DUEL_BUTTON.name(),
                "Take the challenge" + TextConstants.DUEL_ACCEPT_ICON
            },
            {
                LocalizationKeys.DECLINE_DUEL_BUTTON.name(),
                "Decline :open_hands:" //TODO в иконки
            },
        };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
