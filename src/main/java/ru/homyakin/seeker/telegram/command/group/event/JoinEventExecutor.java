package ru.homyakin.seeker.telegram.command.group.event;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.raid.RaidLocalization;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.game.personage.models.errors.EventNotExist;
import ru.homyakin.seeker.game.personage.models.errors.ExpiredEvent;
import ru.homyakin.seeker.game.personage.models.errors.PersonageInOtherEvent;
import ru.homyakin.seeker.game.personage.models.errors.PersonageInThisEvent;

@Component
public class JoinEventExecutor extends CommandExecutor<JoinEvent> {
    private final GroupUserService groupUserService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public JoinEventExecutor(
        GroupUserService groupUserService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(JoinEvent command) {
        final var groupUserPair = groupUserService.getAndActivateOrCreate(
            command.groupId(),
            command.userId()
        );
        final var group = groupUserPair.first();
        final var user = groupUserPair.second();
        final var result = personageService.addEvent(user.personageId(), command.getLaunchedEventId());

        final String notificationText;
        if (result.isRight()) {
            notificationText = RaidLocalization.successJoinEvent(group.language());
        } else {
            final var error = result.getLeft();
            if (error instanceof PersonageInOtherEvent) {
                notificationText = RaidLocalization.userAlreadyInOtherEvent(group.language());
            } else if (error instanceof PersonageInThisEvent) {
                notificationText = RaidLocalization.userAlreadyInThisEvent(group.language());
            } else if (error instanceof EventNotExist) {
                notificationText = CommonLocalization.internalError(group.language());
            } else if (error instanceof ExpiredEvent expiredEvent) {
                notificationText = RaidLocalization.expiredEvent(group.language());
                //TODO может вынести в евент менеджер
                telegramSender.send(TelegramMethods.createEditMessageText(
                    command.groupId(),
                    command.messageId(),
                    expiredEvent.event().toStartMessage(group.language())
                ));
            } else {
                // TODO когда будет паттерн-матчинг для switch - переделать
                notificationText = "ERROR!!!";
            }
        }

        telegramSender.send(TelegramMethods.createAnswerCallbackQuery(command.callbackId(), notificationText));
    }

}

