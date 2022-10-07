package ru.homyakin.seeker.event.service;

import java.util.List;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.chat.Chat;
import ru.homyakin.seeker.event.database.ChatEventGetDao;
import ru.homyakin.seeker.event.database.ChatEventSaveDao;
import ru.homyakin.seeker.event.models.ChatEvent;
import ru.homyakin.seeker.event.models.LaunchedEvent;

@Service
public class ChatEventService {
    private final ChatEventSaveDao chatEventSaveDao;
    private final ChatEventGetDao chatEventGetDao;

    public ChatEventService(ChatEventSaveDao chatEventSaveDao, ChatEventGetDao chatEventGetDao) {
        this.chatEventSaveDao = chatEventSaveDao;
        this.chatEventGetDao = chatEventGetDao;
    }

    public ChatEvent createChatEventDao(LaunchedEvent launchedEvent, Chat chat, Integer messageId) {
        var chatEvent = new ChatEvent(
            launchedEvent.id(),
            chat.id(),
            messageId
        );
        chatEventSaveDao.save(chatEvent);
        return chatEvent;
    }

    public List<ChatEvent> getByLaunchedEventId(Long launchedEventId) {
        return chatEventGetDao.getByLaunchedEventId(launchedEventId);
    }
}
