package ru.homyakin.seeker.command.chat.language;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.chat.ChatService;
import ru.homyakin.seeker.command.CommandExecutor;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.Keyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.user.UserService;

@Component
public class GroupSelectLanguageExecutor extends CommandExecutor<GroupSelectLanguage> {
    private final ChatService chatService;
    private final UserService userService;
    private final TelegramSender telegramSender;

    public GroupSelectLanguageExecutor(
        ChatService chatService,
        UserService userService,
        TelegramSender telegramSender
    ) {
        this.chatService = chatService;
        this.userService = userService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GroupSelectLanguage command) {
        final var chat = chatService.getOrCreate(command.chatId());
        final var language = Language.getOrDefault(command.getLanguageId());
        userService.isUserAdminInChat(command.chatId(), command.userId())
            .peek(isAdmin -> {
                    if (isAdmin) {
                        final var updatedChat = chatService.changeLanguage(chat, language);
                        telegramSender.send(
                            TelegramMethods.createEditMessageText(
                                command.chatId(),
                                command.messageId(),
                                Localization.get(updatedChat.language()).chooseLanguage(),
                                Keyboards.languageKeyboard(updatedChat.language())
                            )
                        );
                    } else {
                        telegramSender.send(
                            TelegramMethods.createAnswerCallbackQuery(
                                command.callbackId(), Localization.get(chat.language()).onlyAdminAction()
                            )
                        );
                    }
                }
            ).peekLeft(error ->
                telegramSender.send(
                    TelegramMethods.createAnswerCallbackQuery(
                        command.callbackId(), Localization.get(chat.language()).internalError()
                    )
                )
            );
    }

}

