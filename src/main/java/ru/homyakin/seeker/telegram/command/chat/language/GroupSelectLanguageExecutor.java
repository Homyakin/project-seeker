package ru.homyakin.seeker.telegram.command.chat.language;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.chat.ChatService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.Keyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class GroupSelectLanguageExecutor extends CommandExecutor<GroupSelectLanguage> {
    private final ChatService chatService;
    private final TelegramSender telegramSender;

    public GroupSelectLanguageExecutor(
        ChatService chatService,
        TelegramSender telegramSender
    ) {
        this.chatService = chatService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GroupSelectLanguage command) {
        final var chat = chatService.getOrCreate(command.chatId());
        final var language = Language.getOrDefault(command.getLanguageId());
        final var updatedChat = chatService.changeLanguage(chat, language);
        telegramSender.send(
            TelegramMethods.createEditMessageText(
                command.chatId(),
                command.messageId(),
                Localization.get(updatedChat.language()).chooseLanguage(),
                Keyboards.languageKeyboard(updatedChat.language())
            )
        );
    }

}

