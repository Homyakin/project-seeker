package ru.homyakin.seeker.telegram.command.user.navigation;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public record StartUser(
    UserId userId,
    Map<String, String> params,
    Instant time
) implements Command {
    public static StartUser from(Message message) {
        final var splitText = message.getText().split(" ");
        final Map<String, String> params;
        if (splitText.length == 1) {
            params = Collections.emptyMap();
        } else {
            /*
             Команда в формате /start=key=value приходит на вход в виде /start key=value
             МАКСИМУМ ОДИН ПАРАМЕТР
             */
            final var paramsArray = splitText[1].split(TextConstants.TG_START_PARAM_DELIMITER);
            if (paramsArray.length == 2) {
                params = Collections.singletonMap(paramsArray[0], paramsArray[1]);
            } else {
                params = Collections.emptyMap();
            }
        }
        return new StartUser(UserId.from(message.getFrom().getId()), params, Instant.ofEpochSecond(message.getDate()));
    }
}
