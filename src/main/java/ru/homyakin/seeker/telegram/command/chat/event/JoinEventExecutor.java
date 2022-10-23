package ru.homyakin.seeker.telegram.command.chat.event;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.telegram.chat.ChatUserService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.game.personage.models.errors.EventNotExist;
import ru.homyakin.seeker.game.personage.models.errors.ExpiredEvent;
import ru.homyakin.seeker.game.personage.models.errors.PersonageInOtherEvent;
import ru.homyakin.seeker.game.personage.models.errors.PersonageInThisEvent;

@Component
public class JoinEventExecutor extends CommandExecutor<JoinEvent> {
    private final ChatUserService chatUserService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public JoinEventExecutor(
        ChatUserService chatUserService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.chatUserService = chatUserService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(JoinEvent command) {
        final var chatUserPair = chatUserService.getAndActivateOrCreate(
            command.chatId(),
            command.userId()
        );
        final var chat = chatUserPair.first();
        final var user = chatUserPair.second();
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
                    expiredEvent.event().toStartMessage(chat.language())
                ));
            } else {
                // TODO когда будет паттерн-матчинг для switch - переделать
                notificationText = "ERROR!!!";
            }
        }

        telegramSender.send(TelegramMethods.createAnswerCallbackQuery(command.callbackId(), notificationText));
    }

}

