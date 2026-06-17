package ru.homyakin.seeker.telegram.user.state;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.command.Command;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public sealed interface UserState permits ChangeNameState, FeedbackState {
    Command nextCommand(Message message);
}
