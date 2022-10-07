package ru.homyakin.seeker.user;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner;
import ru.homyakin.seeker.event.EventService;
import ru.homyakin.seeker.event.launch.LaunchedEventService;
import ru.homyakin.seeker.infrastructure.models.Success;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.infrastructure.models.errors.EitherError;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.user.errors.EventError;
import ru.homyakin.seeker.user.errors.EventNotExist;
import ru.homyakin.seeker.user.errors.ExpiredEvent;
import ru.homyakin.seeker.user.errors.UserInOtherEvent;
import ru.homyakin.seeker.user.errors.UserInThisEvent;

@Component
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final TelegramSender telegramSender;
    private final GetUserDao getUserDao;
    private final SaveUserDao saveUserDao;
    private final UpdateUserDao updateUserDao;
    private final LaunchedEventService launchedEventService;
    private final EventService eventService;

    public UserService(
        TelegramSender telegramSender,
        GetUserDao getUserDao,
        SaveUserDao saveUserDao,
        UpdateUserDao updateUserDao,
        LaunchedEventService launchedEventService,
        EventService eventService
    ) {
        this.telegramSender = telegramSender;
        this.getUserDao = getUserDao;
        this.saveUserDao = saveUserDao;
        this.updateUserDao = updateUserDao;
        this.launchedEventService = launchedEventService;
        this.eventService = eventService;
    }

    public Either<EitherError, Boolean> isUserAdminInChat(Long chatId, Long userId) {
        return telegramSender.send(TelegramMethods.createGetChatMember(chatId, userId))
            .map(it -> it instanceof ChatMemberAdministrator || it instanceof ChatMemberOwner)
            .mapLeft(it -> (EitherError) it); // Без этого преобразования не может сопоставить типы
    }

    public User getOrCreate(Long userId, boolean isPrivateMessage) {
        var user = getUserDao.getById(userId);
        if (user.isPresent() && !user.get().isActivePrivateMessages() && isPrivateMessage) {
            updateUserDao.updateIsActivePrivateMessages(userId, true);
            return getUserDao.getById(userId).orElseThrow();
        } else if (user.isEmpty()) {
            return createUser(userId, isPrivateMessage);
        }
        return user.get();
    }

    public User changeLanguage(User user, Language language) {
        if (!user.isSameLanguage(language)) {
            updateUserDao.updateLanguage(user.id(), language);
            return getUserDao.getById(user.id()).orElseThrow();
        } else {
            return user;
        }
    }

    public Either<EventError, Success> addEvent(User user, Long launchedEventId) {
        final var requestedEvent = launchedEventService.getById(launchedEventId);
        if (requestedEvent.isEmpty()) {
            logger.error("Requested event " + launchedEventId + " doesn't present");
            return Either.left(new EventNotExist());
        } else if (!requestedEvent.get().isActive()){
            return Either.left(eventService.getEventById(requestedEvent.get().eventId())
                .<EventError>map(ExpiredEvent::new)
                .orElseGet(EventNotExist::new)
            );
        }

        final var activeEvent = launchedEventService.getActiveEventByUserId(user.id());
        if (activeEvent.isEmpty()) {
            launchedEventService.addUserToLaunchedEvent(user.id(), launchedEventId);
            return Either.right(new Success());
        }

        if (activeEvent.get().id() == launchedEventId) {
            return Either.left(new UserInThisEvent());
        } else {
            return Either.left(new UserInOtherEvent());
        }
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
