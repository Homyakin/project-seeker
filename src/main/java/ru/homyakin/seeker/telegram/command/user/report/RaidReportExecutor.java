package ru.homyakin.seeker.telegram.command.user.report;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.raid.RaidLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class RaidReportExecutor extends CommandExecutor<RaidReport> {
    private final UserService userService;
    private final PersonageService personageService;
    private final LaunchedEventService launchedEventService;
    private final ItemService itemService;
    private final TelegramSender telegramSender;

    public RaidReportExecutor(
        UserService userService,
        PersonageService personageService,
        LaunchedEventService launchedEventService,
        ItemService itemService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.launchedEventService = launchedEventService;
        this.itemService = itemService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(RaidReport command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var text = personageService.getLastRaidResult(user.personageId())
            .map(
                result -> {
                    final var event = launchedEventService.getById(result.launchedEventId()).orElseThrow();
                    return RaidLocalization.report(
                        user.language(),
                        result,
                        event,
                        result.generatedItemId().flatMap(itemService::getById)
                    );
                }
            )
            .orElseGet(() -> RaidLocalization.reportNotPresentForPersonage(user.language()));
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .build()
        );
    }

}
