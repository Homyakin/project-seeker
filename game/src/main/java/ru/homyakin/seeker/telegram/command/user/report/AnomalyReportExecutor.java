package ru.homyakin.seeker.telegram.command.user.report;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.anomaly.AnomalyLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class AnomalyReportExecutor extends CommandExecutor<AnomalyReport> {
    private final UserService userService;
    private final PersonageService personageService;
    private final LaunchedEventService launchedEventService;
    private final TelegramSender telegramSender;

    public AnomalyReportExecutor(
        UserService userService,
        PersonageService personageService,
        LaunchedEventService launchedEventService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.launchedEventService = launchedEventService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(AnomalyReport command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var text = personageService.getLastAnomalyResult(user.personageId())
            .map(result -> {
                final var event = launchedEventService.getById(result.launchedEventId()).orElseThrow();
                return AnomalyLocalization.report(user.language(), result, event);
            })
            .orElseGet(() -> AnomalyLocalization.reportNotPresentForPersonage(user.language()));
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .build()
        );
    }
}
