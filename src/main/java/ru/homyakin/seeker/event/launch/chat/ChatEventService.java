package ru.homyakin.seeker.event.launch.chat;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.chat.Chat;
import ru.homyakin.seeker.event.launch.LaunchedEvent;

@Service
public class ChatEventService {
    private final SaveChatEventDao saveChatEventDao;

    public ChatEventService(SaveChatEventDao saveChatEventDao) {
        this.saveChatEventDao = saveChatEventDao;
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
}
