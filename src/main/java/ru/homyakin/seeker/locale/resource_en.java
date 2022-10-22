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
                LocalizationKeys.JOIN_EVENT.name(),
                "Join"
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
                    TextConstants.PROFILE_ICON,
                    "%d",
                    TextConstants.EXP_ICON,
                    "%d/%d"
                )
            },
            {
                LocalizationKeys.START_BOSS_EVENT.name(),
                "Boss detected"
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
                
                Available commands (pm and chant):
                %s - change language;
                %s - show profile;
                %s - this message
                
                Только для чата:
                %s - show top by experience in chat
                
                The source code of the game is <a href="%s">here</a>
                """.formatted(
                    CommandType.CHANGE_LANGUAGE.getText(),
                    CommandType.GET_PROFILE.getText(),
                    CommandType.HELP.getText(),
                    CommandType.TOP.getText(),
                    TextConstants.SOURCE_LINK
                )
            },
        };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
