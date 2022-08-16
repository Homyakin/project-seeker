package ru.homyakin.seeker.chat;

import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.models.Chat;
import ru.homyakin.seeker.models.Language;

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

    public Chat setActiveOrCreate(Long chatId) {
        var chat = getChat(chatId);
        if (chat.isPresent() && !chat.get().isActive()) {
            updateChatActive(chatId, true);
            return chat.get().copyWithActive(true);
        } else if (chat.isEmpty()) {
            return createChat(chatId);
        }
        return chat.get();
    }

    public void setNotActive(Long chatId) {
        getChat(chatId).ifPresent(
            chat -> {
                if (chat.isActive()) {
                    updateChatActive(chatId, false);
                }
            }
        );
    }

    private Optional<Chat> getChat(Long chatId) {
        return getChatDao.getById(chatId);
    }

    private Chat createChat(Long chatId) {
        final var chat = new Chat(chatId, true, Language.DEFAULT);
        saveChatDao.save(chat);
        return chat;
    }

    private void updateChatActive(Long chatId, boolean isActive) {
        updateChatDao.updateIsActive(chatId, isActive);
    }
    
}
