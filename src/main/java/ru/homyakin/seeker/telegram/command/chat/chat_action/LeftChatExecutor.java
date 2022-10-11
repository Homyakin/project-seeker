package ru.homyakin.seeker.telegram.command.chat.chat_action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.chat.ChatService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;

@Component
class LeftChatExecutor extends CommandExecutor<LeftChat> {
    private final ChatService chatService;

    public LeftChatExecutor(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public void execute(LeftChat command) {
        chatService.setNotActive(command.chatId());
    }
}
