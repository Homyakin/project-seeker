package ru.homyakin.seeker.telegram.command.group.raid;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.raid.RaidService;
import ru.homyakin.seeker.game.event.raid.models.AddPersonageToRaidError;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.raid.RaidLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class JoinRaidExecutor extends CommandExecutor<JoinRaid> {
    private final GroupUserService groupUserService;
    private final RaidService raidService;
    private final TelegramSender telegramSender;

    public JoinRaidExecutor(
        GroupUserService groupUserService,
        RaidService raidService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.raidService = raidService;
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
        final var result = raidService.addPersonage(user.personageId(), command.launchedEventId());
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
            if (result.get().isExhausted()) {
                telegramSender.send(
                    TelegramMethods.createAnswerCallbackQuery(
                        command.callbackId(),
                        RaidLocalization.exhaustedAlert(group.language())
                    )
                );
            }
        }
    }

    private String mapErrorToUserMessage(AddPersonageToRaidError error, Group group, JoinRaid command) {
        return switch (error) {
            case AddPersonageToRaidError.PersonageInOtherEvent _ ->
                RaidLocalization.userAlreadyInOtherEvent(group.language());
            case AddPersonageToRaidError.RaidNotExist _ -> CommonLocalization.internalError(group.language());
            case AddPersonageToRaidError.PersonageInThisRaid _ ->
                RaidLocalization.userAlreadyInThisRaid(group.language());
            case AddPersonageToRaidError.RaidInProcess _ -> RaidLocalization.raidInProcess(group.language());
            case AddPersonageToRaidError.RaidInFinalStatus raidInFinalStatus -> {
                final var editText = switch (raidInFinalStatus) {
                    case AddPersonageToRaidError.RaidInFinalStatus.CompletedRaid completedRaid ->
                        completedRaid.raid().toEndMessageWithParticipants(
                            completedRaid.participants(),
                            group.language()
                        );
                    case AddPersonageToRaidError.RaidInFinalStatus.CreationErrorRaid _ ->
                        CommonLocalization.internalError(group.language());
                    case AddPersonageToRaidError.RaidInFinalStatus.ExpiredRaid _ ->
                        RaidLocalization.expiredRaid(group.language());
                };
                telegramSender.send(EditMessageTextBuilder.builder()
                    .chatId(command.groupId())
                    .messageId(command.messageId())
                    .text(editText)
                    .build()
                );
                yield RaidLocalization.expiredRaid(group.language());
            }
        };
    }
}

