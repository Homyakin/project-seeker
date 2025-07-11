package ru.homyakin.seeker.telegram;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.models.ChatMemberError;
import ru.homyakin.seeker.telegram.models.TelegramError;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.UserId;

@Component
public class TelegramSender {
    private static final Logger logger = LoggerFactory.getLogger(TelegramSender.class);
    private final TelegramClient client;
    private final GroupTgService groupTgService;
    private final UserService userService;

    protected TelegramSender(TelegramBotConfig botConfig, GroupTgService groupTgService, UserService userService) {
        client = new OkHttpTelegramClient(botConfig.token());
        this.groupTgService = groupTgService;
        this.userService = userService;
    }

    public Either<TelegramError, Message> send(SendMessage sendMessage) {
        try {
            return Either.right(client.execute(sendMessage));
        } catch (Exception e) {
            if (e.getMessage().contains("group chat was upgraded to a supergroup chat")) {
                logger.error("group chat was upgraded to a supergroup chat {}", sendMessage.getChatId());
                groupTgService.setNotActive(GroupTgId.from(sendMessage.getChatId()));
            } else if (e.getMessage().contains("[403] Forbidden: the group chat was deleted")) {
                logger.error("group chat was deleted {}", sendMessage.getChatId());
                groupTgService.setNotActive(GroupTgId.from(sendMessage.getChatId()));
            } else if (e.getMessage().contains("[403] Forbidden: bot is not a member of the supergroup chat")) {
                logger.error("Bot is not a member of the supergroup {}", sendMessage.getChatId());
                groupTgService.setNotActive(GroupTgId.from(sendMessage.getChatId()));
            } else if (e.getMessage().contains("[400] Bad Request: not enough rights to send text messages to the chat")) {
                logger.error("Not enough rights to send text messages to the chat {}", sendMessage.getChatId());
                groupTgService.setNotActive(GroupTgId.from(sendMessage.getChatId()));
            } else if (e.getMessage().contains("[403] Forbidden: bot was blocked by the user")) {
                logger.error("Bot was blocked by the user {}", sendMessage.getChatId());
                userService.deactivatePrivateMessages(UserId.from(sendMessage.getChatId()));
            } else if (e.getMessage().contains("[403] Forbidden: bot was kicked from the group chat")) {
                logger.error("Bot was kicked from the group chat {}", sendMessage.getChatId());
                groupTgService.setNotActive(GroupTgId.from(sendMessage.getChatId()));
            } else {
                logger.error(
                    "Unable send message with text %s to %s".formatted(sendMessage.getText(), sendMessage.getChatId()), e
                );
            }
            return Either.left(new TelegramError.InternalError(e.getMessage()));
        }
    }

    public Either<ChatMemberError, ChatMember> send(GetChatMember getChatMember) {
        try {
            return Either.right(client.execute(getChatMember));
        } catch (Exception e) {
            if (e.getMessage().contains("[400] Bad Request: user not found")) {
                return Either.left(ChatMemberError.UserNotFound.INSTANCE);
            } else if (e.getMessage().contains("[400] Bad Request: PARTICIPANT_ID_INVALID")) {
                logger.error("Invalid chat {} participant {}", getChatMember.getChatId(), getChatMember.getUserId());
                return Either.left(ChatMemberError.InvalidParticipant.INSTANCE);
            } else {
                logger.error(
                    "Unable get chat %s member %d".formatted(getChatMember.getChatId(), getChatMember.getUserId()), e
                );
                return Either.left(new ChatMemberError.InternalError(e.getMessage()));
            }
        }
    }

    public void send(AnswerCallbackQuery callbackQuery) {
        try {
            client.execute(callbackQuery);
        } catch (Exception e) {
            logger.error(
                "Unable answer callback %s".formatted(callbackQuery.getCallbackQueryId()), e
            );
        }
    }

    public void send(EditMessageText editMessageText) {
        try {
            client.execute(editMessageText);
        } catch (Exception e) {
            if (e.getMessage().contains("[403] Forbidden: bot was kicked from the supergroup chat")) {
                logger.error("Bot is not a member of the supergroup {}", editMessageText.getChatId());
                groupTgService.setNotActive(GroupTgId.from(editMessageText.getChatId()));
            } else if (!e.getMessage().contains("Bad Request: message is not modified")) {
                logger.error(
                    "Unable edit message %d in chat %s".formatted(
                        editMessageText.getMessageId(), editMessageText.getChatId()
                    ),
                    e
                );
            }
        }
    }

    public void send(SetMyCommands setMyCommands) throws Exception {
        try {
            client.execute(setMyCommands);
        } catch (Exception e) {
            logger.error("Can't set commands: {}", setMyCommands, e);
            throw e;
        }
    }
}
