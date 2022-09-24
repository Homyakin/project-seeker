package ru.homyakin.seeker.command.chat.event;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.chat.ChatService;
import ru.homyakin.seeker.command.CommandExecutor;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.user.UserService;
import ru.homyakin.seeker.user.errors.EventNotExist;
import ru.homyakin.seeker.user.errors.ExpiredEvent;
import ru.homyakin.seeker.user.errors.UserInOtherEvent;
import ru.homyakin.seeker.user.errors.UserInThisEvent;

@Component
public class JoinEventExecutor extends CommandExecutor<JoinEvent> {
    private final ChatService chatService;
    private final UserService userService;
    private final TelegramSender telegramSender;

    public JoinEventExecutor(
        ChatService chatService,
        UserService userService,
        TelegramSender telegramSender
    ) {
        this.chatService = chatService;
        this.userService = userService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(JoinEvent command) {
        final var chat = chatService.getOrCreate(command.chatId());
        final var user = userService.getOrCreate(command.userId(), false);
        final var result = userService.addEvent(user, command.getLaunchedEventId());

        final String notificationText;
        if (result.isRight()) {
            notificationText = Localization.get(chat.language()).successJoinEvent();
        } else {
            final var error = result.getLeft();
            if (error instanceof UserInOtherEvent) {
                notificationText = Localization.get(chat.language()).userAlreadyInOtherEvent();
            } else if (error instanceof UserInThisEvent) {
                notificationText = Localization.get(chat.language()).userAlreadyInThisEvent();
            } else if (error instanceof EventNotExist) {
                notificationText = Localization.get(chat.language()).internalError();
            } else if (error instanceof ExpiredEvent expiredEvent) {
                notificationText = Localization.get(chat.language()).expiredEvent();
                //TODO может вынести в евент менеджер
                telegramSender.send(TelegramMethods.createEditMessageText(
                    command.chatId(),
                    command.messageId(),
                    command.messageText()
                ));
            } else {
                // TODO когда будет паттерн-матчинг для switch - переделать
                notificationText = "ERROR!!!";
            }
        }

        telegramSender.send(TelegramMethods.createAnswerCallbackQuery(command.callbackId(), notificationText));
    }

}

