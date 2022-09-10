package ru.homyakin.seeker.command.chat_action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.chat.ChatService;
import ru.homyakin.seeker.command.CommandExecutor;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
class JoinChatExecutor extends CommandExecutor<JoinChat> {
    private final ChatService chatService;
    private final TelegramSender telegramSender;

    public JoinChatExecutor(ChatService chatService, TelegramSender telegramSender) {
        this.chatService = chatService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(JoinChat command) {
        final var chat = chatService.setActiveOrCreate(command.chatId());
        telegramSender.send(
            TelegramMethods.createSendMessage(
                chat.id(),
                Localization.get(chat.language()).welcome()
            )
        );
    }
}
