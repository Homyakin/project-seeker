package ru.homyakin.seeker.game.event.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.database.UserEventDao;
import ru.homyakin.seeker.telegram.chat.models.Chat;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.database.LaunchedEventDao;
import ru.homyakin.seeker.game.event.models.ChatEvent;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class LaunchedEventService {
    private final LaunchedEventDao launchedEventDao;
    private final UserEventDao userEventDao;

    private final ChatEventService chatEventService;

    public LaunchedEventService(
        LaunchedEventDao launchedEventDao,
        UserEventDao userEventDao,
        ChatEventService chatEventService
    ) {
        this.launchedEventDao = launchedEventDao;
        this.userEventDao = userEventDao;
        this.chatEventService = chatEventService;
    }

    public LaunchedEvent createLaunchedEvent(Event event) {
        final var id = launchedEventDao.save(event);
        return getById(id).orElseThrow(() -> new IllegalStateException("Launched event must be present after create"));
    }

    public Optional<LaunchedEvent> getById(Long launchedEventId) {
        return launchedEventDao.getById(launchedEventId);
    }

    public ChatEvent addChatMessage(LaunchedEvent launchedEvent, Chat chat, Integer messageId) {
        return chatEventService.createChatEvent(launchedEvent, chat, messageId);
    }

    public void updateActive(LaunchedEvent launchedEvent, boolean isActive) {
        launchedEventDao.updateIsActive(launchedEvent.id(), isActive);
    }

    public Optional<LaunchedEvent> getActiveEventByPersonageId(Long personageId) {
        return launchedEventDao.getActiveByPersonageId(personageId);
    }

    public void addPersonageToLaunchedEvent(Long personageId, Long launchedEventId) {
        userEventDao.save(personageId, launchedEventId);
    }

    public List<LaunchedEvent> getExpiredActiveEvents() {
        return launchedEventDao.getActiveEventsWithLessEndDate(TimeUtils.moscowTime());
    }

    public List<ChatEvent> getChatEvents(LaunchedEvent launchedEvent) {
        return chatEventService.getByLaunchedEventId(launchedEvent.id());
    }
}
