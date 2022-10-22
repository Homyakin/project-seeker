package ru.homyakin.seeker.telegram.command.common;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.chat.ChatService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class HelpExecutor extends CommandExecutor<Help> {
    private final UserService userService;
    private final ChatService chatService;
    private final TelegramSender telegramSender;

    public HelpExecutor(UserService userService, ChatService chatService, TelegramSender telegramSender) {
        this.userService = userService;
        this.chatService = chatService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(Help command) {
        final Language language;
        if (command.isPrivate()) {
            language = userService.getOrCreate(command.chatId(), true).language();
        } else {
            language = chatService.getOrCreate(command.chatId()).language();
        }
        telegramSender.send(TelegramMethods.createSendMessage(command.chatId(), Localization.get(language).help()));
    }
}
