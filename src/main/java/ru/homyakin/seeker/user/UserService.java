package ru.homyakin.seeker.user;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner;
import ru.homyakin.seeker.models.errors.EitherError;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class UserService {
    private final TelegramSender telegramSender;

    public UserService(TelegramSender telegramSender) {
        this.telegramSender = telegramSender;
    }

    public Either<EitherError, Boolean> isUserAdminInChat(Long chatId, Long userId) {
        return telegramSender.send(TelegramMethods.createGetChatMember(chatId, userId))
            .map(it -> it instanceof ChatMemberAdministrator || it instanceof ChatMemberOwner)
            .mapLeft(it -> (EitherError) it); // Без этого преобразования не может сопоставить типы
    }
}
