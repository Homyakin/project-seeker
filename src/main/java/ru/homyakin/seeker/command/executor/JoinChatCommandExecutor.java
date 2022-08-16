package ru.homyakin.seeker.command.executor;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.chat.ChatService;
import ru.homyakin.seeker.command.models.chat_action.JoinChat;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.TelegramUtils;

@Component
class JoinChatCommandExecutor extends CommandExecutor<JoinChat> {
    private final ChatService chatService;
    private final TelegramSender telegramSender;

    JoinChatCommandExecutor(ChatService chatService, TelegramSender telegramSender) {
        this.chatService = chatService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(JoinChat command) {
        final var chat = chatService.setActiveOrCreate(command.chatId());
        telegramSender.send(
            TelegramUtils.createSendMessage(
                chat.id(),
                Localization.get(chat.language()).welcome()
            )
        );
    }
}
