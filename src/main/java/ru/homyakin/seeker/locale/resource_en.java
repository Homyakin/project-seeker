package ru.homyakin.seeker.locale;

import java.util.Collections;
import java.util.HashMap;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

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
                LocalizationKeys.JOIN_RAID_EVENT.name(),
                "Join raid" + TextConstants.RAID_ICON
            },
            {
                LocalizationKeys.RAID_STARTS_PREFIX.name(),
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
                ${profile_icon}${personage_name}
                """
            },
            {
                LocalizationKeys.SUCCESS_RAID.name(),
                "Raid was successful."
            },
            {
                LocalizationKeys.FAILURE_RAID.name(),
                "The seekers failed the raid."
            },
            {
                LocalizationKeys.HELP.name(),
                StringNamedTemplate.format("""
                        Social RPG in Telegram!
                        Just add to the group and join events.
                        Official news channel - ${news_channel_username}.
                                        
                        Available commands (pm and chant):
                        ${language_command} - change language;
                        ${profile_command} - show profile;
                        ${help_command} - this message
                                        
                        Only for group:
                        ${duel_command} - challenge another seeker to a duel. Must be a response to another user message
                                        
                        Only for pm:
                        ${name_command} - change name
                                        
                        The source code of the game is <a href="${github_link}">here</a>
                        """,
                    new HashMap<>() {{
                        put("news_channel_username", TextConstants.TELEGRAM_CHANNEL_USERNAME);
                        put("language_command", CommandType.CHANGE_LANGUAGE.getText());
                        put("profile_command", CommandType.GET_PROFILE.getText());
                        put("help_command", CommandType.HELP.getText());
                        put("duel_command", CommandType.START_DUEL.getText());
                        put("name_command", CommandType.CHANGE_NAME.getText());
                        put("github_link", TextConstants.SOURCE_LINK);
                    }}
                )
            },
            {
                LocalizationKeys.CHANGE_NAME_WITHOUT_NAME.name(),
                StringNamedTemplate.format(
                    "Enter the name separated by a space after the command: \"${name_command} Name\"",
                    Collections.singletonMap("name_command", CommandType.CHANGE_NAME.getText())
                )

            },
            {
                LocalizationKeys.PERSONAGE_NAME_INVALID_LENGTH.name(),
                "The name must be between ${min_name_length} and ${max_name_length} symbols"
            },
            {
                LocalizationKeys.PERSONAGE_NAME_INVALID_SYMBOLS.name(),
                "The name can only contain cyrillic, latin, numbers characters _-.#№: and a space"
            },
            {
                LocalizationKeys.SUCCESS_NAME_CHANGE.name(),
                "Name has been successfully changed!"
            },
            {
                LocalizationKeys.PROFILE_LEVEL_UP.name(),
                StringNamedTemplate.format(
                    "There are unspent leveling points! Press ${level_up_command}",
                    Collections.singletonMap("level_up_command", CommandType.LEVEL_UP.getText())
                )
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
                LocalizationKeys.DUEL_WITH_INITIATOR_LOW_HEALTH.name(),
                "You're barely moving! Go recover first, and then come fight!"
            },
            {
                LocalizationKeys.DUEL_WITH_ACCEPTOR_LOW_HEALTH.name(),
                "Your opponent is dying, and you want to fight him?! Show respect to the wounded!"
            },
            {
                LocalizationKeys.PERSONAGE_ALREADY_START_DUEL.name(),
                "You have already started another duel! Wait for it to finish."
            },
            {
                LocalizationKeys.INIT_DUEL.name(),
                """
                    The seeker <b>${level_icon}${initiating_personage_name} \
                    ${initiating_personage_health}${health_icon}</b> challenges \
                    the seeker <b>${level_icon}${accepting_personage_name} \
                    ${accepting_personage_health}${health_icon}</b>.
                    
                    What will be his answer?
                    """,
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
                """
                    The seeker <b>${level_icon}${winner_personage_name}</b> got the better of \
                    <b>${level_icon}${looser_personage_name}</b>
                    """
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
