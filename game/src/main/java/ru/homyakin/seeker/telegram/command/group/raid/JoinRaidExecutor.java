package ru.homyakin.seeker.telegram.command.group.raid;

import org.springframework.stereotype.Component;

import ru.homyakin.seeker.game.event.raid.RaidService;
import ru.homyakin.seeker.game.event.raid.models.AddPersonageToRaidError;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.raid.RaidLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class JoinRaidExecutor extends CommandExecutor<JoinRaid> {
    private final GroupUserService groupUserService;
    private final RaidService raidService;
    private final ItemService itemService;
    private final TelegramSender telegramSender;

    public JoinRaidExecutor(
        GroupUserService groupUserService,
        RaidService raidService,
        ItemService itemService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.raidService = raidService;
        this.itemService = itemService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(JoinRaid command) {
        final var groupUserPair = groupUserService.getAndActivateOrCreate(
            command.groupTgId(),
            command.userId()
        );
        final var group = groupUserPair.first();
        final var user = groupUserPair.second();
        final var result = raidService.addPersonage(user.personageId(), command.launchedEventId());
        final var text = result.fold(
            error -> mapErrorToUserMessage(error, group, command),
            joinToRaidResult -> RaidLocalization.raidStarting(
                group.language(),
                joinToRaidResult.raid(),
                joinToRaidResult.launchedRaidEvent(),
                joinToRaidResult.participants()
            )
        );
        if (result.isLeft()) {
            telegramSender.send(TelegramMethods.createAnswerCallbackQuery(command.callbackId(), text));
        } else {
            telegramSender.send(
                EditMessageTextBuilder.builder()
                    .chatId(command.groupTgId())
                    .messageId(command.messageId())
                    .text(text)
                    .keyboard(
                        InlineKeyboards.joinRaidKeyboard(
                            group.language(),
                            command.launchedEventId(),
                            result.get().raidEnergyCost()
                        )
                    )
                    .build()
            );
            telegramSender.send(
                TelegramMethods.createAnswerCallbackQuery(
                    command.callbackId(),
                    joinRaidCallbackText(
                        group.language(),
                        result.get().isExhausted(),
                        isBagFull(user.personageId())
                    )
                )
            );
        }
    }

    private String joinRaidCallbackText(Language language, boolean isExhausted, boolean isBagFull) {
        final var builder = new StringBuilder();
        if (isExhausted) {
            builder.append(RaidLocalization.exhaustedAlert(language, isBagFull));
        } else {
            builder.append(RaidLocalization.successJoinRaid(language, isBagFull));
        }
        return builder.toString();
    }

    private boolean isBagFull(PersonageId personageId) {
        return !itemService.getPersonageItems(personageId).hasSpaceInBag();
    }

    private String mapErrorToUserMessage(AddPersonageToRaidError error, GroupTg group, JoinRaid command) {
        return switch (error) {
            case AddPersonageToRaidError.PersonageInOtherEvent _ ->
                RaidLocalization.userAlreadyInOtherEvent(group.language());
            case AddPersonageToRaidError.RaidNotExist _ -> {
                final var editText = RaidLocalization.raidNotFound(group.language());
                telegramSender.send(EditMessageTextBuilder.builder()
                    .chatId(command.groupTgId())
                    .messageId(command.messageId())
                    .text(editText)
                    .build()
                );
                yield editText;
            }
            case AddPersonageToRaidError.PersonageInThisRaid _ ->
                RaidLocalization.userAlreadyInThisRaid(group.language());
            case AddPersonageToRaidError.RaidInProcess _ -> RaidLocalization.raidInProcess(group.language());
            case AddPersonageToRaidError.RaidInFinalStatus raidInFinalStatus -> {
                final var editText = switch (raidInFinalStatus) {
                    case AddPersonageToRaidError.RaidInFinalStatus.CompletedRaid completedRaid ->
                        RaidLocalization.raidBaseMessage(
                            group.language(),
                            completedRaid.raid(),
                            completedRaid.launchedRaidEvent(),
                            completedRaid.participants()
                        );
                    case AddPersonageToRaidError.RaidInFinalStatus.CreationErrorRaid _ ->
                        CommonLocalization.internalError(group.language());
                    case AddPersonageToRaidError.RaidInFinalStatus.ExpiredRaid _ ->
                        RaidLocalization.expiredRaid(group.language());
                };
                telegramSender.send(EditMessageTextBuilder.builder()
                    .chatId(command.groupTgId())
                    .messageId(command.messageId())
                    .text(editText)
                    .build()
                );
                yield RaidLocalization.expiredRaid(group.language());
            }
        };
    }
}

