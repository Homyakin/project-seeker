package ru.homyakin.seeker.command;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.homyakin.seeker.command.models.Command;
import ru.homyakin.seeker.command.models.chat_action.JoinChat;
import ru.homyakin.seeker.command.models.chat_action.LeftChat;

@Component
public class CommandParser {
    public Optional<Command> parse(Update update) {
        Command command = null;
        if (update.hasMyChatMember()) {
           command = switch (update.getMyChatMember().getNewChatMember().getStatus()) {
               case "left" -> new LeftChat(update.getMyChatMember().getChat().getId());
               case "member" -> new JoinChat(update.getMyChatMember().getChat().getId());
               default -> null;
           };
        }

        return Optional.ofNullable(command);
    }

}
