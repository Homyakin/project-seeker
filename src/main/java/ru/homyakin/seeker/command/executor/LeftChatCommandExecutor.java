package ru.homyakin.seeker.command.executor;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.chat.ChatService;
import ru.homyakin.seeker.command.models.chat_action.LeftChat;

@Component
class LeftChatCommandExecutor extends CommandExecutor<LeftChat> {
    private final ChatService chatService;

    LeftChatCommandExecutor(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public void execute(LeftChat command) {
        chatService.setNotActive(command.chatId());
    }
}
