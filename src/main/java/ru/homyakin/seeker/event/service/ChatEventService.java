package ru.homyakin.seeker.event.service;

import java.util.List;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.telegram.chat.Chat;
import ru.homyakin.seeker.event.database.ChatEventDao;
import ru.homyakin.seeker.event.models.ChatEvent;
import ru.homyakin.seeker.event.models.LaunchedEvent;

@Service
public class ChatEventService {
    private final ChatEventDao chatEventDao;

    public ChatEventService(ChatEventDao chatEventDao) {
        this.chatEventDao = chatEventDao;
    }

    public ChatEvent createChatEvent(LaunchedEvent launchedEvent, Chat chat, Integer messageId) {
        var chatEvent = new ChatEvent(
            launchedEvent.id(),
            chat.id(),
            messageId
        );
        chatEventDao.save(chatEvent);
        return chatEvent;
    }

    public List<ChatEvent> getByLaunchedEventId(Long launchedEventId) {
        return chatEventDao.getByLaunchedEventId(launchedEventId);
    }
}
