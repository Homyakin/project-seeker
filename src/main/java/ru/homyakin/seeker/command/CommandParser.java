package ru.homyakin.seeker.command;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.homyakin.seeker.command.chat.event.JoinEvent;
import ru.homyakin.seeker.command.chat.language.GroupChangeLanguage;
import ru.homyakin.seeker.command.chat.chat_action.JoinChat;
import ru.homyakin.seeker.command.chat.chat_action.LeftChat;
import ru.homyakin.seeker.command.chat.language.GroupSelectLanguage;
import ru.homyakin.seeker.command.user.StartUser;
import ru.homyakin.seeker.command.user.language.UserChangeLanguage;
import ru.homyakin.seeker.command.user.language.UserSelectLanguage;
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

    private Optional<Command> parseMessage(Message message) { //TODO разделить на group и private
        if (!message.hasText()) {
            return Optional.empty();
        }
        if (message.isUserMessage()) {
            return parsePrivateMessage(message);
        } else if (TelegramUtils.isGroupMessage(message)) {
            return parseGroupMessage(message);
        } else {
            return Optional.empty();
        }
    }

    private Optional<Command> parsePrivateMessage(Message message) {
        final Command command = switch (message.getText()) {
            case CommandText.CHANGE_LANGUAGE -> new UserChangeLanguage(message.getChatId());
            case CommandText.START -> new StartUser(message.getChatId());
            default -> null;
        };
        return Optional.ofNullable(command);
    }

    private Optional<Command> parseGroupMessage(Message message) {
        final var text = message.getText().split("@")[0];
        final Command command = switch (text) {
            case CommandText.CHANGE_LANGUAGE -> new GroupChangeLanguage(message.getChatId());
            default -> null;
        };
        return Optional.ofNullable(command);
    }

    private Optional<Command> parseCallback(CallbackQuery callback) {
        if (callback.getMessage().isUserMessage()) {
            return parsePrivateCallback(callback);
        } else if (TelegramUtils.isGroupMessage(callback.getMessage())) {
            return parseGroupCallback(callback);
        } else {
            return Optional.empty();
        }
    }

    private Optional<Command> parsePrivateCallback(CallbackQuery callback) {
        final var text = callback.getData().split(CommandText.CALLBACK_DELIMITER)[0];
        final Command command = switch (text) {
            case CommandText.SELECT_LANGUAGE -> new UserSelectLanguage(
                callback.getId(),
                callback.getFrom().getId(),
                callback.getMessage().getMessageId(),
                callback.getData()
            );
            default -> null;
        };
        return Optional.ofNullable(command);
    }

    private Optional<Command> parseGroupCallback(CallbackQuery callback) {
        final var text = callback.getData().split(CommandText.CALLBACK_DELIMITER)[0];
        final Command command = switch (text) {
            case CommandText.SELECT_LANGUAGE -> new GroupSelectLanguage(
                callback.getId(),
                callback.getMessage().getChatId(),
                callback.getMessage().getMessageId(),
                callback.getFrom().getId(),
                callback.getData()
            );
            case CommandText.JOIN_EVENT -> new JoinEvent(
                callback.getId(),
                callback.getMessage().getChatId(),
                callback.getMessage().getMessageId(),
                callback.getFrom().getId(),
                callback.getData()
            );
            default -> null;
        };
        return Optional.ofNullable(command);
    }

}
