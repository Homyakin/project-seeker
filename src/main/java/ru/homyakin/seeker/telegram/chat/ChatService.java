package ru.homyakin.seeker.telegram.chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class ChatService {
    private final ChatGetDao chatGetDao;
    private final ChatSaveDao chatSaveDao;
    private final ChatUpdateDao chatUpdateDao;

    public ChatService(ChatGetDao chatGetDao, ChatSaveDao chatSaveDao, ChatUpdateDao chatUpdateDao) {
        this.chatGetDao = chatGetDao;
        this.chatSaveDao = chatSaveDao;
        this.chatUpdateDao = chatUpdateDao;
    }

    public Chat getOrCreate(Long chatId) {
        var chat = getChat(chatId);
        if (chat.isPresent() && !chat.get().isActive()) {
            chatUpdateDao.updateIsActive(chatId, true);
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
                    chatUpdateDao.updateIsActive(chatId, false);
                }
            }
        );
    }

    public Chat changeLanguage(Chat chat, Language language) {
        if (!chat.isSameLanguage(language)) {
            chatUpdateDao.updateLanguage(chat.id(), language);
            return getChat(chat.id()).orElseThrow();
        } else {
            return chat;
        }
    }

    public List<Chat> getGetChatsWithLessNextEventDate(LocalDateTime maxNextEventDate) {
        return chatGetDao.getGetChatsWithLessNextEventDate(maxNextEventDate);
    }

    public void updateNextEventDate(Long chatId, LocalDateTime nextEventDate) {
        chatUpdateDao.updateNextEventDate(chatId, nextEventDate);
    }

    private Optional<Chat> getChat(Long chatId) {
        return chatGetDao.getById(chatId);
    }

    private Chat createChat(Long chatId) {
        final var chat = new Chat(chatId, true, Language.DEFAULT, TimeUtils.moscowTime());
        chatSaveDao.save(chat);
        return chat;
    }
    
}
