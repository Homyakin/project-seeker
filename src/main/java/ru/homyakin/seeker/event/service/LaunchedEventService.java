package ru.homyakin.seeker.event.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.chat.Chat;
import ru.homyakin.seeker.event.models.Event;
import ru.homyakin.seeker.event.database.LaunchedEventGetDao;
import ru.homyakin.seeker.event.database.LaunchedEventSaveDao;
import ru.homyakin.seeker.event.database.UserEventSaveDao;
import ru.homyakin.seeker.event.database.LaunchedEventUpdateDao;
import ru.homyakin.seeker.event.models.ChatEvent;
import ru.homyakin.seeker.event.models.LaunchedEvent;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class LaunchedEventService {
    private final LaunchedEventSaveDao launchedEventSaveDao;
    private final LaunchedEventGetDao launchedEventGetDao;
    private final LaunchedEventUpdateDao launchedEventUpdateDao;
    private final UserEventSaveDao userEventSaveDao;

    private final ChatEventService chatEventService;

    public LaunchedEventService(
        LaunchedEventSaveDao launchedEventSaveDao,
        LaunchedEventGetDao launchedEventGetDao,
        LaunchedEventUpdateDao launchedEventUpdateDao,
        UserEventSaveDao userEventSaveDao,
        ChatEventService chatEventService
    ) {
        this.launchedEventSaveDao = launchedEventSaveDao;
        this.launchedEventGetDao = launchedEventGetDao;
        this.launchedEventUpdateDao = launchedEventUpdateDao;
        this.userEventSaveDao = userEventSaveDao;
        this.chatEventService = chatEventService;
    }

    public LaunchedEvent createLaunchedEvent(Event event) {
        final var id = launchedEventSaveDao.save(event);
        return getById(id).orElseThrow(() -> new IllegalStateException("Launched event must be present after create"));
    }

    public Optional<LaunchedEvent> getById(Long launchedEventId) {
        return launchedEventGetDao.getById(launchedEventId);
    }

    public ChatEvent addChatMessage(LaunchedEvent launchedEvent, Chat chat, Integer messageId) {
        return chatEventService.createChatEventDao(launchedEvent, chat, messageId);
    }

    public void updateActive(LaunchedEvent launchedEvent, boolean isActive) {
        launchedEventUpdateDao.updateIsActive(launchedEvent.id(), isActive);
    }

    public Optional<LaunchedEvent> getActiveEventByUserId(Long userId) {
        return launchedEventGetDao.getActiveByUserId(userId);
    }

    public void addUserToLaunchedEvent(Long userId, Long launchedEventId) {
        userEventSaveDao.save(userId, launchedEventId);
    }

    public List<LaunchedEvent> getExpiredActiveEvents() {
        return launchedEventGetDao.getActiveEventsWithLessEndDate(TimeUtils.moscowTime());
    }

    public List<ChatEvent> getChatEvents(LaunchedEvent launchedEvent) {
        return chatEventService.getByLaunchedEventId(launchedEvent.id());
    }
}
