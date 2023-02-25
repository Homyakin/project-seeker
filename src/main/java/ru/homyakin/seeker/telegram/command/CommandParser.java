package ru.homyakin.seeker.telegram.command;

import com.vdurmont.emoji.EmojiParser;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.homyakin.seeker.telegram.command.group.duel.AcceptDuel;
import ru.homyakin.seeker.telegram.command.group.duel.DeclineDuel;
import ru.homyakin.seeker.telegram.command.group.duel.StartDuel;
import ru.homyakin.seeker.telegram.command.group.language.GroupChangeLanguage;
import ru.homyakin.seeker.telegram.command.group.action.JoinGroup;
import ru.homyakin.seeker.telegram.command.group.action.LeftGroup;
import ru.homyakin.seeker.telegram.command.group.language.GroupSelectLanguage;
import ru.homyakin.seeker.telegram.command.group.profile.GetProfileInGroup;
import ru.homyakin.seeker.telegram.command.common.Help;
import ru.homyakin.seeker.telegram.command.group.tavern_menu.GetTavernMenu;
import ru.homyakin.seeker.telegram.command.group.tavern_menu.Order;
import ru.homyakin.seeker.telegram.command.user.level.CharacteristicType;
import ru.homyakin.seeker.telegram.command.user.level.CharacteristicUp;
import ru.homyakin.seeker.telegram.command.user.level.LevelUp;
import ru.homyakin.seeker.telegram.command.user.StartUser;
import ru.homyakin.seeker.telegram.command.user.language.UserChangeLanguage;
import ru.homyakin.seeker.telegram.command.user.language.UserSelectLanguage;
import ru.homyakin.seeker.telegram.command.group.event.JoinEvent;
import ru.homyakin.seeker.telegram.command.user.profile.ChangeName;
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
        if (TelegramUtils.isGroupChat(chatMember.getChat())) {
            return Optional.ofNullable(
                switch (chatMember.getNewChatMember().getStatus()) {
                    case "left" -> new LeftGroup(chatMember.getChat().getId());
                    case "member" -> new JoinGroup(chatMember.getChat().getId());
                    default -> null;
                }
            );
        } else {
            //TODO обработка сообщений из лички
            return Optional.empty();
        }
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
        return CommandType.getFromString(
                EmojiParser.parseToAliases(message.getText().split(" ")[0])
            )
            .map(commandType -> switch (commandType) {
                case CHANGE_LANGUAGE -> new UserChangeLanguage(message.getChatId());
                case START -> new StartUser(message.getChatId());
                case GET_PROFILE -> new GetProfileInPrivate(message.getChatId());
                case HELP -> new Help(message.getChatId(), true);
                case CHANGE_NAME -> new ChangeName(message.getChatId(), message.getText());
                case LEVEL_UP -> new LevelUp(message.getChatId());
                case UP_STRENGTH -> new CharacteristicUp(message.getChatId(), CharacteristicType.STRENGTH);
                case UP_AGILITY -> new CharacteristicUp(message.getChatId(), CharacteristicType.AGILITY);
                case UP_WISDOM -> new CharacteristicUp(message.getChatId(), CharacteristicType.WISDOM);
                default -> null;
            });
    }

    private Optional<Command> parseGroupMessage(Message message) {
        return CommandType.getFromString(message.getText().split("@")[0].split(" ")[0])
            .map(commandType -> switch (commandType) {
                case CHANGE_LANGUAGE -> new GroupChangeLanguage(message.getChatId());
                case GET_PROFILE -> new GetProfileInGroup(
                    message.getChatId(),
                    message.getFrom().getId()
                );
                case HELP -> new Help(message.getChatId(), false);
                case START_DUEL -> new StartDuel(
                    message.getChatId(),
                    message.getFrom().getId(),
                    Optional.ofNullable(message.getReplyToMessage()).map(
                        it -> new StartDuel.ReplyInfo(it.getMessageId(), it.getFrom().getId(), it.getFrom().getIsBot())
                    )
                );
                case TAVERN_MENU -> new GetTavernMenu(message.getChatId());
                case ORDER -> new Order(message.getChatId(), message.getFrom().getId(), message.getText().split("@")[0]);
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
                case DECLINE_DUEL -> new DeclineDuel(
                    callback.getId(),
                    callback.getMessage().getChatId(),
                    callback.getFrom().getId(),
                    callback.getMessage().getMessageId(),
                    callback.getData()
                );
                case ACCEPT_DUEL -> new AcceptDuel(
                    callback.getId(),
                    callback.getMessage().getChatId(),
                    callback.getFrom().getId(),
                    callback.getMessage().getMessageId(),
                    callback.getData()
                );
                default -> null;
            });
    }

}
