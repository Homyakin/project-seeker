package ru.homyakin.seeker.game.event.service;

import java.util.List;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.database.ChatLaunchedEventDao;
import ru.homyakin.seeker.game.event.models.ChatLaunchedEvent;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.telegram.chat.models.Chat;

@Service
public class ChatEventService {
    private final ChatLaunchedEventDao chatLaunchedEventDao;

    public ChatEventService(ChatLaunchedEventDao chatLaunchedEventDao) {
        this.chatLaunchedEventDao = chatLaunchedEventDao;
    }

    public ChatLaunchedEvent createChatEvent(LaunchedEvent launchedEvent, Chat chat, Integer messageId) {
        var chatEvent = new ChatLaunchedEvent(
            launchedEvent.id(),
            chat.id(),
            messageId
        );
        chatLaunchedEventDao.save(chatEvent);
        return chatEvent;
    }

    public List<ChatLaunchedEvent> getByLaunchedEventId(Long launchedEventId) {
        return chatLaunchedEventDao.getByLaunchedEventId(launchedEventId);
    }
}
