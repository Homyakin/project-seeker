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
                LocalizationKeys.TOP_PERSONAGES_BY_EXP_IN_CHAT.name(),
                TextConstants.EXP_ICON + "Top chat personages by experience:%n%s"
            },
            {
                LocalizationKeys.HELP.name(),
                """
                Social RPG in Telegram!
                Just add to the chat and join events.
                Official news channel - %s.
                
                Available commands (pm and chant):
                %s - change language;
                %s - show profile;
                %s - this message
                
                Only for chat:
                %s - show top by experience in chat
                
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
        };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
