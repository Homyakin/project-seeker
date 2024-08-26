package ru.homyakin.seeker.telegram.command.group.raid;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.errors.AddPersonageToRaidError;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.raid.RaidLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

import java.util.Optional;

@Component
public class JoinRaidExecutor extends CommandExecutor<JoinRaid> {
    private final GroupUserService groupUserService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public JoinRaidExecutor(
        GroupUserService groupUserService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(JoinRaid command) {
        final var groupUserPair = groupUserService.getAndActivateOrCreate(
            command.groupId(),
            command.userId()
        );
        final var group = groupUserPair.first();
        final var user = groupUserPair.second();
        final var result = personageService.joinRaid(user.personageId(), command.launchedEventId());
        final var text = result.fold(
            error -> mapErrorToUserMessage(error, group, command),
            joinToRaidResult -> joinToRaidResult.toMessage(group.language())
        );
        if (result.isLeft()) {
            telegramSender.send(TelegramMethods.createAnswerCallbackQuery(command.callbackId(), text));
        } else {
            telegramSender.send(
                EditMessageTextBuilder.builder()
                    .chatId(command.groupId())
                    .messageId(command.messageId())
                    .text(text)
                    .keyboard(InlineKeyboards.joinRaidKeyboard(group.language(), command.launchedEventId()))
                    .build()
            );
        }
    }

    private String mapErrorToUserMessage(AddPersonageToRaidError error, Group group, JoinRaid command) {
        return switch (error) {
            case AddPersonageToRaidError.PersonageInOtherEvent _ ->
                RaidLocalization.userAlreadyInOtherEvent(group.language());
            case AddPersonageToRaidError.RaidNotExist _ -> CommonLocalization.internalError(group.language());
            case AddPersonageToRaidError.EndedRaid expiredEvent -> {
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
            case AddPersonageToRaidError.PersonageInThisRaid _ ->
                RaidLocalization.userAlreadyInThisRaid(group.language());
            case AddPersonageToRaidError.RaidInProcess _ -> RaidLocalization.raidInProcess(group.language());
            case AddPersonageToRaidError.NotEnoughEnergy notEnoughEnergy ->
                RaidLocalization.notEnoughEnergy(group.language(), notEnoughEnergy);
        };
    }
}

