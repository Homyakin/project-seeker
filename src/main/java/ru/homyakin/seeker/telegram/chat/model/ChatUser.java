package ru.homyakin.seeker.telegram.chat.model;

import ru.homyakin.seeker.telegram.chat.database.ChatUserDao;

//TODO добавить обработку выхода из чата
public record ChatUser(
    long chatId,
    long userId,
    boolean isActive
) {
    public ChatUser activate(ChatUserDao chatUserDao) {
        return changeActive(true, chatUserDao);
    }

    public ChatUser deactivate(ChatUserDao chatUserDao) {
        return changeActive(false, chatUserDao);
    }

    private ChatUser changeActive(boolean newActive, ChatUserDao chatUserDao) {
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
}
