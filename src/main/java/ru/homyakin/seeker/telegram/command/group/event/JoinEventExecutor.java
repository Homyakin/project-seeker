package ru.homyakin.seeker.telegram.command.group.event;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.errors.EventNotExist;
import ru.homyakin.seeker.game.personage.models.errors.ExpiredEvent;
import ru.homyakin.seeker.game.personage.models.errors.PersonageEventError;
import ru.homyakin.seeker.game.personage.models.errors.PersonageInOtherEvent;
import ru.homyakin.seeker.game.personage.models.errors.PersonageInThisEvent;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.raid.RaidLocalization;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

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
        final var result = personageService.addEvent(user.personageId(), command.launchedEventId());

        final var notificationText = result.fold(
            error -> mapErrorToUserMessage(error, group, command),
            success -> RaidLocalization.successJoinEvent(group.language())
        );

        telegramSender.send(TelegramMethods.createAnswerCallbackQuery(command.callbackId(), notificationText));
    }

    private String mapErrorToUserMessage(PersonageEventError error, Group group, JoinEvent command) {
        return switch (error) {
            case PersonageInOtherEvent ignored -> RaidLocalization.userAlreadyInOtherEvent(group.language());
            case EventNotExist ignored -> CommonLocalization.internalError(group.language());
            case ExpiredEvent expiredEvent -> {
                //TODO может вынести в евент менеджер
                telegramSender.send(EditMessageTextBuilder.builder()
                    .chatId(command.groupId())
                    .messageId(command.messageId())
                    .text(expiredEvent.event().toStartMessage(group.language()))
                    .build()
                );
                yield RaidLocalization.expiredRaid(group.language());
            }
            case PersonageInThisEvent ignored -> RaidLocalization.userAlreadyInThisEvent(group.language());
            case PersonageEventError.EventInProcess ignored -> RaidLocalization.raidInProcess(group.language());
        };
    }
}

