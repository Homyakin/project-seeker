package ru.homyakin.seeker.telegram.command.group.event;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.raid.RaidService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.errors.PersonageEventError;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.raid.RaidLocalization;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

import java.util.Optional;

@Component
public class JoinEventExecutor extends CommandExecutor<JoinEvent> {
    private final GroupUserService groupUserService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;
    private final RaidService raidService;

    public JoinEventExecutor(
        GroupUserService groupUserService,
        PersonageService personageService,
        TelegramSender telegramSender,
        RaidService raidService
    ) {
        this.groupUserService = groupUserService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
        this.raidService = raidService;
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
        final var text = result.fold(
            error -> mapErrorToUserMessage(error, group, command),
            launchedEvent -> {
                final var participants = personageService.getByLaunchedEvent(command.launchedEventId());
                return raidService.getByEventId(launchedEvent.eventId())
                    .map(it -> it.toStartMessage(group.language(), launchedEvent.startDate(), launchedEvent.endDate()))
                    .map(it -> it + "\n\n" + RaidLocalization.raidParticipants(group.language(), participants))
                    .orElseThrow();
            }
        );
        if (result.isLeft()) {
            telegramSender.send(TelegramMethods.createAnswerCallbackQuery(command.callbackId(), text));
        } else {
            telegramSender.send(
                EditMessageTextBuilder.builder()
                    .chatId(command.groupId())
                    .messageId(command.messageId())
                    .text(text)
                    .keyboard(InlineKeyboards.joinRaidEventKeyboard(group.language(), command.launchedEventId()))
                    .build()
            );
        }
    }

    private String mapErrorToUserMessage(PersonageEventError error, Group group, JoinEvent command) {
        return switch (error) {
            case PersonageEventError.PersonageInOtherEvent _ ->
                RaidLocalization.userAlreadyInOtherEvent(group.language());
            case PersonageEventError.EventNotExist _ -> CommonLocalization.internalError(group.language());
            case PersonageEventError.ExpiredEvent expiredEvent -> {
                //TODO может вынести в евент менеджер
                final var editText = Optional.ofNullable(
                    switch (expiredEvent.launchedEvent().status()) {
                        case LAUNCHED -> null; // по идее сюда мы не должны никогда попасть
                        case EXPIRED -> RaidLocalization.expiredRaid(group.language());
                        case FAILED, SUCCESS -> expiredEvent.raid().toEndMessageWithParticipants(
                            personageService.getByLaunchedEvent(command.launchedEventId()),
                            group.language()
                        );
                        case CREATION_ERROR -> CommonLocalization.internalError(group.language());
                    }
                );
                editText.ifPresent(text -> telegramSender.send(EditMessageTextBuilder.builder()
                    .chatId(command.groupId())
                    .messageId(command.messageId())
                    .text(text)
                    .build()
                ));
                yield RaidLocalization.expiredRaid(group.language());
            }
            case PersonageEventError.PersonageInThisEvent _ ->
                RaidLocalization.userAlreadyInThisEvent(group.language());
            case PersonageEventError.EventInProcess _ -> RaidLocalization.raidInProcess(group.language());
            case PersonageEventError.NotEnoughEnergy notEnoughEnergy ->
                RaidLocalization.notEnoughEnergy(group.language(), notEnoughEnergy);
        };
    }
}

