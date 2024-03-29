package ru.homyakin.seeker.telegram;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import ru.homyakin.seeker.telegram.models.TelegramError;

@Component
public class TelegramSender extends DefaultAbsSender {
    private static final Logger logger = LoggerFactory.getLogger(TelegramSender.class);
    private final String token;

    protected TelegramSender(TelegramBotConfig botConfig, DefaultBotOptions options) {
        super(options);
        this.token = botConfig.token();
    }

    public Either<TelegramError, Message> send(SendMessage sendMessage) {
        try {
            return Either.right(execute(sendMessage));
        } catch (Exception e) {
            logger.error(
                "Unable send message with text %s to %s".formatted(sendMessage.getText(), sendMessage.getChatId()), e
            );
            return Either.left(new TelegramError.InternalError(e.getMessage()));
        }
    }

    public Either<TelegramError, ChatMember> send(GetChatMember getChatMember) {
        try {
            return Either.right(execute(getChatMember));
        } catch (Exception e) {
            if (e.getMessage().contains("[400] Bad Request: user not found")) {
                return Either.left(TelegramError.UserNotFound.INSTANCE);
            } else {
                logger.error(
                    "Unable get chat %s member %d".formatted(getChatMember.getChatId(), getChatMember.getUserId()), e
                );
                return Either.left(new TelegramError.InternalError(e.getMessage()));
            }
        }
    }

    public void send(AnswerCallbackQuery callbackQuery) {
        try {
            execute(callbackQuery);
        } catch (Exception e) {
            logger.error(
                "Unable answer callback %s".formatted(callbackQuery.getCallbackQueryId()), e
            );
        }
    }

    public void send(EditMessageText editMessageText) {
        try {
            execute(editMessageText);
        } catch (Exception e) {
            if (!e.getMessage().contains("Bad Request: message is not modified")) {
                logger.error(
                    "Unable edit message %d in chat %s".formatted(
                        editMessageText.getMessageId(), editMessageText.getChatId()
                    ),
                    e
                );
            }
        }
    }

    @Override
    public String getBotToken() {
        return token;
    }
}
