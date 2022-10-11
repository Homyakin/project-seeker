package ru.homyakin.seeker.telegram.user;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberAdministrator;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberOwner;
import ru.homyakin.seeker.character.CharacterService;
import ru.homyakin.seeker.event.service.EventService;
import ru.homyakin.seeker.event.service.LaunchedEventService;
import ru.homyakin.seeker.infrastructure.models.Success;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.infrastructure.models.errors.EitherError;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.user.model.error.EventError;
import ru.homyakin.seeker.telegram.user.model.error.ExpiredEvent;
import ru.homyakin.seeker.telegram.user.model.error.UserInOtherEvent;
import ru.homyakin.seeker.telegram.user.model.error.UserInThisEvent;
import ru.homyakin.seeker.telegram.user.model.User;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.telegram.user.model.error.EventNotExist;

@Component
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final TelegramSender telegramSender;
    private final UserDao userDao;
    private final LaunchedEventService launchedEventService;
    private final EventService eventService;
    private final CharacterService characterService;

    public UserService(
        TelegramSender telegramSender,
        UserDao userDao,
        LaunchedEventService launchedEventService,
        EventService eventService,
        CharacterService characterService
    ) {
        this.telegramSender = telegramSender;
        this.userDao = userDao;
        this.launchedEventService = launchedEventService;
        this.eventService = eventService;
        this.characterService = characterService;
    }

    public Either<EitherError, Boolean> isUserAdminInChat(Long chatId, Long userId) {
        return telegramSender.send(TelegramMethods.createGetChatMember(chatId, userId))
            .map(it -> it instanceof ChatMemberAdministrator || it instanceof ChatMemberOwner)
            .mapLeft(it -> (EitherError) it); // Без этого преобразования не может сопоставить типы
    }

    public User getOrCreate(Long userId, boolean isPrivateMessage) {
        return userDao
            .getById(userId)
            .map(user -> {
                if (isPrivateMessage) {
                    return user.activatePrivateMessages(userDao);
                }
                return user;
            })
            .orElseGet(() -> createUser(userId, isPrivateMessage));
    }

    public User changeLanguage(User user, Language language) {
        return user.changeLanguage(language, userDao);
    }

    public Either<EventError, Success> addEvent(User user, Long launchedEventId) {
        final var requestedEvent = launchedEventService.getById(launchedEventId);
        if (requestedEvent.isEmpty()) {
            logger.error("Requested event " + launchedEventId + " doesn't present");
            return Either.left(new EventNotExist());
        } else if (!requestedEvent.get().isActive()) {
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
        final var character = characterService.createCharacter();
        final var user = new User(
            userId,
            isPrivateMessage,
            Language.DEFAULT,
            character.id()
        );
        userDao.save(user);
        return user;
    }
}
