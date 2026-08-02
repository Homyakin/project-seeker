package ru.homyakin.seeker.telegram.command.group.anomaly;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.anomaly.action.AnomalyService;
import ru.homyakin.seeker.locale.anomaly.AnomalyLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.anomaly.TelegramAnomalyService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.AnomalyKeyboards;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class AnomalyReadyExecutor extends CommandExecutor<AnomalyReady> {
    private final GroupUserService groupUserService;
    private final AnomalyService anomalyService;
    private final TelegramAnomalyService telegramAnomalyService;
    private final TelegramSender telegramSender;

    public AnomalyReadyExecutor(
        GroupUserService groupUserService,
        AnomalyService anomalyService,
        TelegramAnomalyService telegramAnomalyService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.anomalyService = anomalyService;
        this.telegramAnomalyService = telegramAnomalyService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(AnomalyReady command) {
        final var pair = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId());
        final var group = pair.first();
        final var user = pair.second();
        final var result = anomalyService.ready(command.launchedEventId(), user.personageId());
        if (result.isLeft()) {
            telegramSender.send(TelegramMethods.createAnswerCallbackQuery(
                command.callbackId(),
                AnomalyLocalization.error(group.language(), result.getLeft())
            ));
            return;
        }
        switch (result.get()) {
            case AnomalyService.AnomalyReadyResult.StartedPveWaiting pveWaiting -> {
                final var event = pveWaiting.launchedEvent();
                final var anomaly = anomalyService.findAnomaly(event.id()).orElseThrow();
                telegramSender.send(
                    EditMessageTextBuilder.builder()
                        .chatId(command.groupTgId())
                        .messageId(command.messageId())
                        .text(telegramAnomalyService.eventText(
                            group.language(), event, anomaly, group.domainGroupId()
                        ))
                        .keyboard(AnomalyKeyboards.forEvent(
                            group.language(), event.id(), anomaly, group.domainGroupId()
                        ))
                        .build()
                );
            }
            case AnomalyService.AnomalyReadyResult.StartedSearching searching -> {
                final var event = searching.launchedEvent();
                final var anomaly = anomalyService.findAnomaly(event.id()).orElseThrow();
                telegramSender.send(
                    EditMessageTextBuilder.builder()
                        .chatId(command.groupTgId())
                        .messageId(command.messageId())
                        .text(telegramAnomalyService.eventText(
                            group.language(), event, anomaly, group.domainGroupId()
                        ))
                        .keyboard(AnomalyKeyboards.forEvent(
                            group.language(), event.id(), anomaly, group.domainGroupId()
                        ))
                        .build()
                );
            }
            case AnomalyService.AnomalyReadyResult.BattleCompleted battle ->
                telegramAnomalyService.notifyBattleFinished(battle.result());
        }
        telegramSender.send(TelegramMethods.createAnswerCallbackQuery(command.callbackId(), ""));
    }
}
