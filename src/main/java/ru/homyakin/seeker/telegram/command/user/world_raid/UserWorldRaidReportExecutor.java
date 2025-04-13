package ru.homyakin.seeker.telegram.command.user.world_raid;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.world_raid.action.PersonageWorldRaidBattleResultCommand;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.world_raid.WorldRaidLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class UserWorldRaidReportExecutor extends CommandExecutor<UserWorldRaidReport> {
    private final UserService userService;
    private final LaunchedEventService launchedEventService;
    private final ItemService itemService;
    private final PersonageWorldRaidBattleResultCommand personageWorldRaidBattleResultCommand;
    private final TelegramSender telegramSender;

    public UserWorldRaidReportExecutor(
        UserService userService,
        LaunchedEventService launchedEventService,
        ItemService itemService,
        PersonageWorldRaidBattleResultCommand personageWorldRaidBattleResultCommand,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.launchedEventService = launchedEventService;
        this.itemService = itemService;
        this.personageWorldRaidBattleResultCommand = personageWorldRaidBattleResultCommand;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(UserWorldRaidReport command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var result = personageWorldRaidBattleResultCommand.getForLastWorldRaid(user.personageId());
        final String text;
        if (result.isEmpty()) {
            text = WorldRaidLocalization.personageWorldRaidReportNotFound(user.language());
        } else {
            text = CommonLocalization.personageBattleReport(
                user.language(),
                result.get(),
                launchedEventService.getById(result.get().launchedEventId()).orElseThrow(),
                result.get().generatedItemId().flatMap(itemService::getById)
            );
        }
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(command.userId())
            .text(text)
            .build()
        );
    }
}
