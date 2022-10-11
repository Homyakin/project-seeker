package ru.homyakin.seeker.telegram.command.chat.language;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.chat.ChatService;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.Keyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class GroupChangeLanguageExecutor extends CommandExecutor<GroupChangeLanguage> {
    private final ChatService chatService;
    private final TelegramSender telegramSender;

    public GroupChangeLanguageExecutor(
        ChatService chatService,
        TelegramSender telegramSender
    ) {
        this.chatService = chatService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GroupChangeLanguage command) {
        final var chat = chatService.getOrCreate(command.chatId());
        telegramSender.send(
            TelegramMethods.createSendMessage(
                command.chatId(),
                Localization.get(chat.language()).chooseLanguage(),
                Keyboards.languageKeyboard(chat.language())
            )
        );
    }
}
