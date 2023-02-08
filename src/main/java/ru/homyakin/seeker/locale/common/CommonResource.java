package ru.homyakin.seeker.locale.common;

import java.util.HashMap;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public record CommonResource(
    String welcomeGroup,
    String welcomeUser,
    String chooseLanguage,
    String onlyAdminLanguage,
    String internalError,
    String help,
    String profileTemplate
) {
    public String profile(Personage personage) {
        final var params = new HashMap<String, Object>() {{
            put("profile_icon", TextConstants.PROFILE_ICON);
            put("level_icon", TextConstants.LEVEL_ICON);
            put("exp_icon", TextConstants.EXP_ICON);
            put("personage_name", personage.name());
        }};
        return StringNamedTemplate.format(
            profileTemplate,
            params
        );
    }

    public String help() {
        return StringNamedTemplate.format(
            help,
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
}
