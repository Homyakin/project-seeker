package ru.homyakin.seeker.telegram.command.chat.event;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.telegram.chat.model.ChatUser;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.chat.ChatService;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.game.personage.model.error.EventNotExist;
import ru.homyakin.seeker.game.personage.model.error.ExpiredEvent;
import ru.homyakin.seeker.game.personage.model.error.PersonageInOtherEvent;
import ru.homyakin.seeker.game.personage.model.error.PersonageInThisEvent;

@Component
public class JoinEventExecutor extends CommandExecutor<JoinEvent> {
    private final ChatService chatService;
    private final UserService userService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public JoinEventExecutor(
        ChatService chatService,
        UserService userService,
        PersonageService personageService, TelegramSender telegramSender
    ) {
        this.chatService = chatService;
        this.userService = userService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(JoinEvent command) {
        // TODO возможно надо объединить эти три пункта в один, чтобы не забывать
        final var chat = chatService.getOrCreate(command.chatId());
        final var user = userService.getOrCreate(command.userId(), false);
        ChatUser.getByKey(chat.id(), user.id())
            .ifPresentOrElse(
                ChatUser::activate,
                () -> new ChatUser(chat.id(), user.id(), true).save()
            );
        final var result = personageService.addEvent(user.personageId(), command.getLaunchedEventId());

        final String notificationText;
        if (result.isRight()) {
            notificationText = Localization.get(chat.language()).successJoinEvent();
        } else {
            final var error = result.getLeft();
            if (error instanceof PersonageInOtherEvent) {
                notificationText = Localization.get(chat.language()).userAlreadyInOtherEvent();
            } else if (error instanceof PersonageInThisEvent) {
                notificationText = Localization.get(chat.language()).userAlreadyInThisEvent();
            } else if (error instanceof EventNotExist) {
                notificationText = Localization.get(chat.language()).internalError();
            } else if (error instanceof ExpiredEvent expiredEvent) {
                notificationText = Localization.get(chat.language()).expiredEvent();
                //TODO может вынести в евент менеджер
                telegramSender.send(TelegramMethods.createEditMessageText(
                    command.chatId(),
                    command.messageId(),
                    expiredEvent.event().getLocaleByLanguageOrDefault(chat.language()).toEndMessage()
                ));
            } else {
                // TODO когда будет паттерн-матчинг для switch - переделать
                notificationText = "ERROR!!!";
            }
        }

        telegramSender.send(TelegramMethods.createAnswerCallbackQuery(command.callbackId(), notificationText));
    }

}

