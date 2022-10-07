package ru.homyakin.seeker.event.service;

import java.util.List;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.chat.Chat;
import ru.homyakin.seeker.event.database.GetChatEventDao;
import ru.homyakin.seeker.event.database.SaveChatEventDao;
import ru.homyakin.seeker.event.models.ChatEvent;
import ru.homyakin.seeker.event.models.LaunchedEvent;

@Service
public class ChatEventService {
    private final SaveChatEventDao saveChatEventDao;
    private final GetChatEventDao getChatEventDao;

    public ChatEventService(SaveChatEventDao saveChatEventDao, GetChatEventDao getChatEventDao) {
        this.saveChatEventDao = saveChatEventDao;
        this.getChatEventDao = getChatEventDao;
    }

    public ChatEvent createChatEventDao(LaunchedEvent launchedEvent, Chat chat, Integer messageId) {
        var chatEvent = new ChatEvent(
            launchedEvent.id(),
            chat.id(),
            messageId
        );
        saveChatEventDao.save(chatEvent);
        return chatEvent;
    }

    public List<ChatEvent> getByLaunchedEventId(Long launchedEventId) {
        return getChatEventDao.getByLaunchedEventId(launchedEventId);
    }
}
