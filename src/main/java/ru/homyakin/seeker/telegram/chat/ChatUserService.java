package ru.homyakin.seeker.telegram.chat;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.utils.models.Pair;
import ru.homyakin.seeker.telegram.chat.database.ChatUserDao;
import ru.homyakin.seeker.telegram.chat.models.Chat;
import ru.homyakin.seeker.telegram.chat.models.ChatUser;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;

@Service
public class ChatUserService {
    private final ChatService chatService;
    private final UserService userService;
    private final ChatUserDao chatUserDao;

    public ChatUserService(ChatService chatService, UserService userService, ChatUserDao chatUserDao) {
        this.chatService = chatService;
        this.userService = userService;
        this.chatUserDao = chatUserDao;
    }

    public Pair<Chat, User> getAndActivateOrCreate(long chatId, long userId) {
        final var chat = chatService.getOrCreate(chatId);
        final var user = userService.getOrCreateFromChat(userId);
        chatUserDao.getByChatIdAndUserId(chatId, userId)
            .ifPresentOrElse(
                chatUser -> chatUser.activate(chatUserDao),
                () -> chatUserDao.save(new ChatUser(chatId, userId, true))
            );
        return new Pair<>(chat, user);
    }
}
