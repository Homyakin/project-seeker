package ru.homyakin.seeker.telegram.command.group.anomaly;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.anomaly.action.AnomalyService;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.locale.anomaly.AnomalyLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.anomaly.TelegramAnomalyService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.AnomalyKeyboards;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class AnomalyMenuExecutor extends CommandExecutor<AnomalyMenu> {
    private final GroupUserService groupUserService;
    private final GetGroup getGroup;
    private final AnomalyService anomalyService;
    private final TelegramAnomalyService telegramAnomalyService;
    private final TelegramSender telegramSender;

    public AnomalyMenuExecutor(
        GroupUserService groupUserService,
        GetGroup getGroup,
        AnomalyService anomalyService,
        TelegramAnomalyService telegramAnomalyService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.getGroup = getGroup;
        this.anomalyService = anomalyService;
        this.telegramAnomalyService = telegramAnomalyService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(AnomalyMenu command) {
        final var group = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId()).first();
        final var domainGroupId = group.domainGroupId();
        if (!anomalyService.isEligibleForMenu(domainGroupId)) {
            telegramSender.send(TelegramMethods.createAnswerCallbackQuery(
                command.callbackId(),
                AnomalyLocalization.error(
                    group.language(),
                    ru.homyakin.seeker.game.event.anomaly.entity.AnomalyError.NoStormScanner.INSTANCE
                )
            ));
            return;
        }
        final var active = anomalyService.findActive(domainGroupId);
        if (active.isPresent()) {
            final var event = active.get();
            final var anomaly = anomalyService.findAnomaly(event.id()).orElseThrow();
            telegramSender.send(
                EditMessageTextBuilder.builder()
                    .chatId(command.groupTgId())
                    .messageId(command.messageId())
                    .text(telegramAnomalyService.eventText(group.language(), event, anomaly))
                    .keyboard(AnomalyKeyboards.forEvent(group.language(), event.id(), anomaly))
                    .build()
            );
        } else {
            final var isRegistered = getGroup.forceGet(domainGroupId).isRegistered();
            final var canStartToday = anomalyService.canStartToday(domainGroupId);
            telegramSender.send(
                EditMessageTextBuilder.builder()
                    .chatId(command.groupTgId())
                    .messageId(command.messageId())
                    .text(AnomalyLocalization.menu(group.language(), canStartToday, isRegistered))
                    .keyboard(AnomalyKeyboards.menuKeyboard(group.language(), isRegistered && canStartToday))
                    .build()
            );
        }
        telegramSender.send(TelegramMethods.createAnswerCallbackQuery(command.callbackId(), ""));
    }
}
