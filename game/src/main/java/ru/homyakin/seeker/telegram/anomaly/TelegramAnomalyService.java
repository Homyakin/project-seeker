package ru.homyakin.seeker.telegram.anomaly;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.battle.BattleVisualizerConfig;
import ru.homyakin.seeker.game.event.anomaly.action.AnomalyService;
import ru.homyakin.seeker.game.event.anomaly.entity.Anomaly;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyConfig;
import ru.homyakin.seeker.game.event.anomaly.entity.SendAnomalyChallengeToGroup;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.service.GroupEventService;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.anomaly.AnomalyLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.AnomalyKeyboards;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class TelegramAnomalyService implements SendAnomalyChallengeToGroup {
    private final GroupTgService groupTgService;
    private final GetGroup getGroup;
    private final TelegramSender telegramSender;
    private final GroupEventService groupEventService;
    private final AnomalyService anomalyService;
    private final AnomalyConfig anomalyConfig;
    private final BattleVisualizerConfig battleVisualizerConfig;

    public TelegramAnomalyService(
        GroupTgService groupTgService,
        GetGroup getGroup,
        TelegramSender telegramSender,
        GroupEventService groupEventService,
        AnomalyService anomalyService,
        AnomalyConfig anomalyConfig,
        BattleVisualizerConfig battleVisualizerConfig
    ) {
        this.groupTgService = groupTgService;
        this.getGroup = getGroup;
        this.telegramSender = telegramSender;
        this.groupEventService = groupEventService;
        this.anomalyService = anomalyService;
        this.anomalyConfig = anomalyConfig;
        this.battleVisualizerConfig = battleVisualizerConfig;
    }

    @Override
    public void send(GroupId groupId, LaunchedEvent challengedEvent, LaunchedEvent searchingEvent) {
        if (!getGroup.forceGet(groupId).isActive()) {
            return;
        }
        final var groupTg = groupTgService.forceGet(groupId);
        final var anomaly = anomalyService.findAnomaly(challengedEvent.id()).orElseThrow();
        final var participants = anomalyService.participants(challengedEvent.id()).list();
        final var result = telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(groupTg.id())
                .text(AnomalyLocalization.challenge(
                    groupTg.language(),
                    participants,
                    anomalyConfig.partySize()
                ))
                .keyboard(AnomalyKeyboards.forEvent(groupTg.language(), challengedEvent.id(), anomaly))
                .build()
        );
        result.peek(message ->
            groupEventService.createGroupEvent(challengedEvent.id(), groupTg, message.getMessageId())
        );
    }

    public String eventText(Language language, LaunchedEvent event, Anomaly anomaly) {
        final var participants = anomalyService.participants(event.id()).list();
        return switch (anomaly.phase()) {
            case CHOOSING_MODE -> AnomalyLocalization.discovered(language);
            case GATHERING -> AnomalyLocalization.gathering(
                language, anomaly, participants, anomalyConfig.partySize()
            );
            case SEARCHING -> AnomalyLocalization.searching(
                language, participants, anomalyConfig.partySize(), event
            );
            case CHALLENGED -> AnomalyLocalization.challenge(
                language, participants, anomalyConfig.partySize()
            );
        };
    }

    public void notifyExpired(LaunchedEvent event, EventResult.AnomalyResult result) {
        groupEventService.getByLaunchedEventId(event.id()).forEach(groupEvent -> {
            final var group = groupTgService.getOrCreate(groupEvent.groupId());
            final var text = switch (result) {
                case EventResult.AnomalyResult.ExpiredChoosingOrGathering _ ->
                    AnomalyLocalization.expired(group.language());
                case EventResult.AnomalyResult.NoMatch _ ->
                    AnomalyLocalization.noMatch(group.language(), anomalyConfig.noMatchReward());
                case EventResult.AnomalyResult.AlreadyFinal _ ->
                    AnomalyLocalization.expired(group.language());
                case EventResult.AnomalyResult.BattleFinished _ ->
                    AnomalyLocalization.expired(group.language());
            };
            telegramSender.send(
                EditMessageTextBuilder.builder()
                    .chatId(group.id())
                    .messageId(groupEvent.messageId())
                    .text(text)
                    .build()
            );
        });
    }

    public void notifyBattleFinished(EventResult.AnomalyResult.BattleFinished result) {
        final var link = battleVisualizerConfig.battleUrl(result.winnerLaunchedEventId());
        notifyBattleSide(result.winnerLaunchedEventId(), true, link);
        notifyBattleSide(result.loserLaunchedEventId(), false, link);
    }

    private void notifyBattleSide(long launchedEventId, boolean victory, String link) {
        groupEventService.getByLaunchedEventId(launchedEventId).forEach(groupEvent -> {
            final var group = groupTgService.getOrCreate(groupEvent.groupId());
            final var text = victory
                ? AnomalyLocalization.battleVictory(
                    group.language(), anomalyConfig.victoryReward(), link
                )
                : AnomalyLocalization.battleDefeat(
                    group.language(), anomalyConfig.defeatReward(), link
                );
            telegramSender.send(
                EditMessageTextBuilder.builder()
                    .chatId(group.id())
                    .messageId(groupEvent.messageId())
                    .text(text)
                    .build()
            );
        });
    }
}
