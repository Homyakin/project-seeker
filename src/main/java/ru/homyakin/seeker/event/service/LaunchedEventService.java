package ru.homyakin.seeker.event.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.chat.Chat;
import ru.homyakin.seeker.event.models.Event;
import ru.homyakin.seeker.event.database.GetLaunchedEventDao;
import ru.homyakin.seeker.event.database.SaveLaunchedEventDao;
import ru.homyakin.seeker.event.database.SaveUserEventDao;
import ru.homyakin.seeker.event.database.UpdateLaunchedEventDao;
import ru.homyakin.seeker.event.models.ChatEvent;
import ru.homyakin.seeker.event.models.LaunchedEvent;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class LaunchedEventService {
    private final SaveLaunchedEventDao saveLaunchedEventDao;
    private final GetLaunchedEventDao getLaunchedEventDao;
    private final UpdateLaunchedEventDao updateLaunchedEventDao;
    private final SaveUserEventDao saveUserEventDao;

    private final ChatEventService chatEventService;

    public LaunchedEventService(
        SaveLaunchedEventDao saveLaunchedEventDao,
        GetLaunchedEventDao getLaunchedEventDao,
        UpdateLaunchedEventDao updateLaunchedEventDao,
        SaveUserEventDao saveUserEventDao,
        ChatEventService chatEventService
    ) {
        this.saveLaunchedEventDao = saveLaunchedEventDao;
        this.getLaunchedEventDao = getLaunchedEventDao;
        this.updateLaunchedEventDao = updateLaunchedEventDao;
        this.saveUserEventDao = saveUserEventDao;
        this.chatEventService = chatEventService;
    }

    public LaunchedEvent createLaunchedEvent(Event event) {
        final var id = saveLaunchedEventDao.save(event);
        return getById(id).orElseThrow(() -> new IllegalStateException("Launched event must be present after create"));
    }

    public Optional<LaunchedEvent> getById(Long launchedEventId) {
        return getLaunchedEventDao.getById(launchedEventId);
    }

    public ChatEvent addChatMessage(LaunchedEvent launchedEvent, Chat chat, Integer messageId) {
        return chatEventService.createChatEventDao(launchedEvent, chat, messageId);
    }

    public void updateActive(LaunchedEvent launchedEvent, boolean isActive) {
        updateLaunchedEventDao.updateIsActive(launchedEvent.id(), isActive);
    }

    public Optional<LaunchedEvent> getActiveEventByUserId(Long userId) {
        return getLaunchedEventDao.getActiveByUserId(userId);
    }

    public void addUserToLaunchedEvent(Long userId, Long launchedEventId) {
        saveUserEventDao.save(userId, launchedEventId);
    }

    public List<LaunchedEvent> getExpiredActiveEvents() {
        return getLaunchedEventDao.getActiveEventsWithLessEndDate(TimeUtils.moscowTime());
    }

    public List<ChatEvent> getChatEvents(LaunchedEvent launchedEvent) {
        return chatEventService.getByLaunchedEventId(launchedEvent.id());
    }
}
