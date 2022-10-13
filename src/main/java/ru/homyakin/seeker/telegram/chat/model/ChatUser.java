package ru.homyakin.seeker.telegram.chat.model;

import java.util.Optional;
import ru.homyakin.seeker.telegram.chat.database.ChatUserDao;

//TODO добавить обработку выхода из чата
public record ChatUser(
    long chatId,
    long userId,
    boolean isActive
) {
    private static ChatUserDao chatUserDao;

    public static Optional<ChatUser> getByKey(long chatId, long userId) {
        return chatUserDao.getByChatIdAndUserId(chatId, userId);
    }

    public void save() {
        chatUserDao.save(this);
    }

    public ChatUser activate() {
        return changeActive(true);
    }

    public ChatUser deactivate() {
        return changeActive(false);
    }

    private ChatUser changeActive(boolean newActive) {
        if (isActive != newActive) {
            final var chatUser = new ChatUser(
                chatId,
                userId,
                newActive
            );
            chatUserDao.update(chatUser);
            return chatUser;
        }
        return this;
    }

    public static void setChatUserDao(ChatUserDao newChatUserDao) {
        if (chatUserDao == null) {
            chatUserDao = newChatUserDao;
        } else {
            throw new IllegalStateException("Chat user dao is already set");
        }
    }
}
