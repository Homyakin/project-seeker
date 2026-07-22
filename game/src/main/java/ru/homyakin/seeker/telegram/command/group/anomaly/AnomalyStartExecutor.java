package ru.homyakin.seeker.telegram.command.group.anomaly;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.anomaly.action.AnomalyService;
import ru.homyakin.seeker.game.event.service.GroupEventService;
import ru.homyakin.seeker.locale.anomaly.AnomalyLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.anomaly.TelegramAnomalyService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.AnomalyKeyboards;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class AnomalyStartExecutor extends CommandExecutor<AnomalyStart> {
    private final GroupUserService groupUserService;
    private final AnomalyService anomalyService;
    private final TelegramAnomalyService telegramAnomalyService;
    private final GroupEventService groupEventService;
    private final TelegramSender telegramSender;

    public AnomalyStartExecutor(
        GroupUserService groupUserService,
        AnomalyService anomalyService,
        TelegramAnomalyService telegramAnomalyService,
        GroupEventService groupEventService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.anomalyService = anomalyService;
        this.telegramAnomalyService = telegramAnomalyService;
        this.groupEventService = groupEventService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(AnomalyStart command) {
        final var pair = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId());
        final var group = pair.first();
        final var user = pair.second();
        final var result = anomalyService.start(group.domainGroupId(), user.personageId());
        if (result.isLeft()) {
            telegramSender.send(TelegramMethods.createAnswerCallbackQuery(
                command.callbackId(),
                AnomalyLocalization.error(group.language(), result.getLeft())
            ));
            return;
        }
        final var event = result.get();
        final var anomaly = anomalyService.findAnomaly(event.id()).orElseThrow();
        groupEventService.createGroupEvent(event.id(), group, command.messageId());
        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(command.groupTgId())
                .messageId(command.messageId())
                .text(telegramAnomalyService.eventText(group.language(), event, anomaly))
                .keyboard(AnomalyKeyboards.forEvent(group.language(), event.id(), anomaly))
                .build()
        );
        telegramSender.send(TelegramMethods.createAnswerCallbackQuery(command.callbackId(), ""));
    }
}
