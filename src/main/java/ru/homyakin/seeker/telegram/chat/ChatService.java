package ru.homyakin.seeker.telegram.chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class ChatService {
    private final ChatDao chatDao;

    public ChatService(ChatDao chatDao) {
        this.chatDao = chatDao;
    }

    public Chat getOrCreate(Long chatId) {
        var chat = getChat(chatId);
        if (chat.isPresent() && !chat.get().isActive()) {
            chatDao.updateIsActive(chatId, true);
            return getChat(chatId).orElseThrow();
        } else if (chat.isEmpty()) {
            return createChat(chatId);
        }
        return chat.get();
    }

    public void setNotActive(Long chatId) {
        getChat(chatId).ifPresent(
            chat -> {
                if (chat.isActive()) {
                    chatDao.updateIsActive(chatId, false);
                }
            }
        );
    }

    public Chat changeLanguage(Chat chat, Language language) {
        if (!chat.isSameLanguage(language)) {
            chatDao.updateLanguage(chat.id(), language);
            return getChat(chat.id()).orElseThrow();
        } else {
            return chat;
        }
    }

    public List<Chat> getGetChatsWithLessNextEventDate(LocalDateTime maxNextEventDate) {
        return chatDao.getGetChatsWithLessNextEventDate(maxNextEventDate);
    }

    public void updateNextEventDate(Long chatId, LocalDateTime nextEventDate) {
        chatDao.updateNextEventDate(chatId, nextEventDate);
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
