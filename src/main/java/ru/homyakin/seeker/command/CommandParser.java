package ru.homyakin.seeker.command;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.homyakin.seeker.command.language.GroupChangeLanguage;
import ru.homyakin.seeker.command.chat_action.JoinChat;
import ru.homyakin.seeker.command.chat_action.LeftChat;
import ru.homyakin.seeker.command.language.GroupSelectLanguage;
import ru.homyakin.seeker.telegram.utils.TelegramUtils;

@Component
public class CommandParser {
    public Optional<Command> parse(Update update) {
        final Optional<Command> command;
        if (update.hasMyChatMember()) {
            command = parseMyChatMember(update.getMyChatMember());
        } else if (update.hasMessage()) {
            command = parseMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            command = parseCallback(update.getCallbackQuery());
        } else {
            command = Optional.empty();
        }

        return command;
    }

    private Optional<Command> parseMyChatMember(ChatMemberUpdated chatMember) {
        return Optional.ofNullable(
            switch (chatMember.getNewChatMember().getStatus()) {
                case "left" -> new LeftChat(chatMember.getChat().getId());
                case "member" -> new JoinChat(chatMember.getChat().getId());
                default -> null;
            }
        );
    }

    private Optional<Command> parseMessage(Message message) {
        if (!message.hasText()) {
            return Optional.empty();
        }
        final var text = message.getText();
        if (text.startsWith(CommandText.CHANGE_LANGUAGE.text())) {
            if (TelegramUtils.isGroupMessage(message)) {
                return Optional.of(new GroupChangeLanguage(message.getChatId()));
            }
        }
        return Optional.empty();
    }

    private Optional<Command> parseCallback(CallbackQuery callback) {
        if (callback.getData().startsWith(CommandText.SELECT_LANGUAGE.text())) {
            if (TelegramUtils.isGroupMessage(callback.getMessage())) {
                return Optional.of(
                    new GroupSelectLanguage(
                        callback.getId(),
                        callback.getMessage().getChatId(),
                        callback.getMessage().getMessageId(),
                        callback.getFrom().getId(),
                        callback.getData()
                    )
                );
            }
        }
        return Optional.empty();
    }

}
