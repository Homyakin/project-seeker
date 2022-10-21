package ru.homyakin.seeker.telegram.command;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.homyakin.seeker.telegram.command.chat.language.GroupChangeLanguage;
import ru.homyakin.seeker.telegram.command.chat.chat_action.JoinChat;
import ru.homyakin.seeker.telegram.command.chat.chat_action.LeftChat;
import ru.homyakin.seeker.telegram.command.chat.language.GroupSelectLanguage;
import ru.homyakin.seeker.telegram.command.chat.profile.GetProfileInChat;
import ru.homyakin.seeker.telegram.command.chat.top.Top;
import ru.homyakin.seeker.telegram.command.user.StartUser;
import ru.homyakin.seeker.telegram.command.user.language.UserChangeLanguage;
import ru.homyakin.seeker.telegram.command.user.language.UserSelectLanguage;
import ru.homyakin.seeker.telegram.command.chat.event.JoinEvent;
import ru.homyakin.seeker.telegram.command.user.profile.GetProfileInPrivate;
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
        return CommandType.getFromString(message.getText())
            .map(commandType -> switch (commandType) {
            case CHANGE_LANGUAGE -> new UserChangeLanguage(message.getChatId());
            case START -> new StartUser(message.getChatId());
            case GET_PROFILE -> new GetProfileInPrivate(message.getChatId());
            default -> null;
        });
    }

    private Optional<Command> parseGroupMessage(Message message) {
        return CommandType.getFromString(message.getText().split("@")[0].split(" ")[0])
            .map(commandType -> switch (commandType) {
                case CHANGE_LANGUAGE -> new GroupChangeLanguage(message.getChatId());
                case GET_PROFILE -> new GetProfileInChat(
                    message.getChatId(),
                    message.getFrom().getId()
                );
                case TOP -> new Top(message.getChatId(), message.getFrom().getId());
                default -> null;
            });
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
        final var text = callback.getData().split(CommandType.CALLBACK_DELIMITER)[0];
        return CommandType.getFromString(text)
            .map(commandType -> switch (commandType) {
                case SELECT_LANGUAGE -> new UserSelectLanguage(
                    callback.getId(),
                    callback.getFrom().getId(),
                    callback.getMessage().getMessageId(),
                    callback.getData()
                );
                default -> null;
            });
    }

    private Optional<Command> parseGroupCallback(CallbackQuery callback) {
        final var text = callback.getData().split(CommandType.CALLBACK_DELIMITER)[0];
        return CommandType.getFromString(text)
            .map(commandType -> switch (commandType) {
                case SELECT_LANGUAGE -> new GroupSelectLanguage(
                    callback.getId(),
                    callback.getMessage().getChatId(),
                    callback.getMessage().getMessageId(),
                    callback.getFrom().getId(),
                    callback.getData()
                );
                case JOIN_EVENT -> new JoinEvent(
                    callback.getId(),
                    callback.getMessage().getChatId(),
                    callback.getMessage().getMessageId(),
                    callback.getFrom().getId(),
                    callback.getData()
                );
                default -> null;
            });
    }

}
