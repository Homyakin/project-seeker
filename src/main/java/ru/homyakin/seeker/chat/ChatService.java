package ru.homyakin.seeker.chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class ChatService {
    private final GetChatDao getChatDao;
    private final SaveChatDao saveChatDao;
    private final UpdateChatDao updateChatDao;

    public ChatService(GetChatDao getChatDao, SaveChatDao saveChatDao, UpdateChatDao updateChatDao) {
        this.getChatDao = getChatDao;
        this.saveChatDao = saveChatDao;
        this.updateChatDao = updateChatDao;
    }

    public Chat getOrCreate(Long chatId) {
        var chat = getChat(chatId);
        if (chat.isPresent() && !chat.get().isActive()) {
            updateChatDao.updateIsActive(chatId, true);
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
                    updateChatDao.updateIsActive(chatId, false);
                }
            }
        );
    }

    public Chat changeLanguage(Chat chat, Language language) {
        if (!chat.isSameLanguage(language)) {
            updateChatDao.updateLanguage(chat.id(), language);
            return getChat(chat.id()).orElseThrow();
        } else {
            return chat;
        }
    }

    public List<Chat> getGetChatsWithLessNextEventDate(LocalDateTime maxNextEventDate) {
        return getChatDao.getGetChatsWithLessNextEventDate(maxNextEventDate);
    }

    public void updateNextEventDate(Long chatId, LocalDateTime nextEventDate) {
        updateChatDao.updateNextEventDate(chatId, nextEventDate);
    }

    private Optional<Chat> getChat(Long chatId) {
        return getChatDao.getById(chatId);
    }

    private Chat createChat(Long chatId) {
        final var chat = new Chat(chatId, true, Language.DEFAULT, TimeUtils.moscowTime());
        saveChatDao.save(chat);
        return chat;
    }
    
}
