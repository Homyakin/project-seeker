package ru.homyakin.seeker.telegram.chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.chat.database.ChatDao;
import ru.homyakin.seeker.telegram.chat.models.Chat;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class ChatService {
    private final ChatDao chatDao;

    public ChatService(ChatDao chatDao) {
        this.chatDao = chatDao;
    }

    public Chat getOrCreate(Long chatId) {
        return getChat(chatId)
            .map(chat -> chat.activate(chatDao))
            .orElseGet(() -> createChat(chatId));
    }

    public void setNotActive(Long chatId) {
        getChat(chatId).map(chat -> chat.deactivate(chatDao));
    }

    public Chat changeLanguage(Chat chat, Language language) {
        return chat.changeLanguage(language, chatDao);
    }

    public List<Chat> getGetChatsWithLessNextEventDate(LocalDateTime maxNextEventDate) {
        return chatDao.getGetChatsWithLessNextEventDate(maxNextEventDate);
    }

    public void updateNextEventDate(Chat chat, LocalDateTime nextEventDate) {
        chat.updateNextEventDate(nextEventDate, chatDao);
    }

    private Optional<Chat> getChat(Long chatId) {
        return chatDao.getById(chatId);
    }

    private Chat createChat(Long chatId) {
        final var chat = new Chat(chatId, true, Language.DEFAULT, TimeUtils.moscowTime());
        chatDao.save(chat);
        return chat;
    }
    
}
