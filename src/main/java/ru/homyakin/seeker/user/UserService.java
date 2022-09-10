package ru.homyakin.seeker.user;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.models.errors.EitherError;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class UserService {
    private final TelegramSender telegramSender;
    private final GetUserDao getUserDao;
    private final SaveUserDao saveUserDao;
    private final UpdateUserDao updateUserDao;

    public UserService(
        TelegramSender telegramSender,
        GetUserDao getUserDao,
        SaveUserDao saveUserDao,
        UpdateUserDao updateUserDao
    ) {
        this.telegramSender = telegramSender;
        this.getUserDao = getUserDao;
        this.saveUserDao = saveUserDao;
        this.updateUserDao = updateUserDao;
    }

    public Either<EitherError, Boolean> isUserAdminInChat(Long chatId, Long userId) {
        return telegramSender.send(TelegramMethods.createGetChatMember(chatId, userId))
            .map(it -> it instanceof ChatMemberAdministrator || it instanceof ChatMemberOwner)
            .mapLeft(it -> (EitherError) it); // Без этого преобразования не может сопоставить типы
    }

    public User getOrCreateUser(Long userId, boolean isPrivateMessage) {
        var user = getUserDao.getById(userId);
        if (user.isPresent() && !user.get().isActivePrivateMessages() && isPrivateMessage) {
            updateUserDao.updateIsActivePrivateMessages(userId, true);
            return getUserDao.getById(userId).orElseThrow();
        } else if (user.isEmpty()) {
            return createUser(userId, isPrivateMessage);
        }
        return user.get();
    }

    private User createUser(Long userId, boolean isPrivateMessage) {
        final var user = new User(
            userId,
            isPrivateMessage,
            Language.DEFAULT
        );
        saveUserDao.save(user);
        return user;
    }
}
