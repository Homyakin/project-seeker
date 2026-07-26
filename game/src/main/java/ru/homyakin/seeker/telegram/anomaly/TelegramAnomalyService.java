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
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.anomaly.AnomalyLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.utils.AnomalyKeyboards;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
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
        if (!(anomalyService.findAnomaly(challengedEvent.id()).orElseThrow() instanceof Anomaly.Challenged anomaly)) {
            throw new IllegalStateException("Expected challenged anomaly for event " + challengedEvent.id());
        }
        final var groupTg = groupTgService.forceGet(groupId);
        final var participants = anomalyService.participants(challengedEvent.id()).list();
        final var result = telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(groupTg.id())
                .text(AnomalyLocalization.challenge(
                    groupTg.language(),
                    anomaly,
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
        return switch (anomaly) {
            case Anomaly.Safe safe when safe.phase() == Anomaly.Safe.Phase.GATHERING ->
                AnomalyLocalization.gathering(
                    language, safe, participants, anomalyConfig.partySize(), event
                );
            case Anomaly.Safe safe when safe.phase() == Anomaly.Safe.Phase.PVE_WAITING ->
                AnomalyLocalization.pveWaiting(
                    language, safe, participants, anomalyConfig.partySize(), event
                );
            case Anomaly.Dangerous dangerous when dangerous.phase() == Anomaly.Dangerous.Phase.GATHERING ->
                AnomalyLocalization.gathering(
                    language, dangerous, participants, anomalyConfig.partySize(), event
                );
            case Anomaly.Dangerous dangerous when dangerous.phase() == Anomaly.Dangerous.Phase.SEARCHING ->
                AnomalyLocalization.searching(
                    language, participants, anomalyConfig.partySize(), event
                );
            case Anomaly.Challenged challenged ->
                AnomalyLocalization.challenge(
                    language, challenged, participants, anomalyConfig.partySize()
                );
            case Anomaly.Safe _, Anomaly.Dangerous _ -> AnomalyLocalization.expired(language);
        };
    }

    public void notifyExpired(LaunchedEvent event, EventResult.AnomalyResult result) {
        switch (result) {
            case EventResult.AnomalyResult.PveBattleFinished pve ->
                notifyPveBattleFinished(pve);
            case EventResult.AnomalyResult.BattleFinished battle ->
                notifyBattleFinished(battle);
            case EventResult.AnomalyResult.NoMatch noMatch ->
                replyToGroupEvents(
                    noMatch.launchedEventId(),
                    group -> AnomalyLocalization.noMatch(group.language(), anomalyConfig.noMatchReward())
                );
            case EventResult.AnomalyResult.ExpiredGathering _,
                 EventResult.AnomalyResult.AlreadyFinal _ ->
                replyToGroupEvents(
                    event.id(),
                    group -> AnomalyLocalization.expired(group.language())
                );
        }
    }

    public void notifyBattleFinished(EventResult.AnomalyResult.BattleFinished result) {
        final var link = battleVisualizerConfig.battleUrl(result.winnerLaunchedEventId());
        notifyBattleSide(result.winnerLaunchedEventId(), true, link, anomalyConfig.victoryReward());
        notifyBattleSide(result.loserLaunchedEventId(), false, link, anomalyConfig.defeatReward());
    }

    private void notifyPveBattleFinished(EventResult.AnomalyResult.PveBattleFinished result) {
        final var link = battleVisualizerConfig.battleUrl(result.launchedEventId());
        groupEventService.getByLaunchedEventId(result.launchedEventId()).forEach(groupEvent -> {
            final var group = groupTgService.getOrCreate(groupEvent.groupId());
            telegramSender.send(
                SendMessageBuilder.builder()
                    .chatId(group.id())
                    .replyMessageId(groupEvent.messageId())
                    .text(AnomalyLocalization.pveBattleResult(group.language(), result))
                    .keyboard(InlineKeyboards.battleVisualizerKeyboard(group.language(), link))
                    .build()
            );
        });
    }

    private void notifyBattleSide(
        long launchedEventId,
        boolean victory,
        String link,
        Money reward
    ) {
        groupEventService.getByLaunchedEventId(launchedEventId).forEach(groupEvent -> {
            final var group = groupTgService.getOrCreate(groupEvent.groupId());
            final var text = victory
                ? AnomalyLocalization.battleVictory(group.language(), reward)
                : AnomalyLocalization.battleDefeat(group.language(), reward);
            telegramSender.send(
                SendMessageBuilder.builder()
                    .chatId(group.id())
                    .replyMessageId(groupEvent.messageId())
                    .text(text)
                    .keyboard(InlineKeyboards.battleVisualizerKeyboard(group.language(), link))
                    .build()
            );
        });
    }

    private void replyToGroupEvents(
        long launchedEventId,
        java.util.function.Function<GroupTg, String> text
    ) {
        groupEventService.getByLaunchedEventId(launchedEventId).forEach(groupEvent -> {
            final var group = groupTgService.getOrCreate(groupEvent.groupId());
            telegramSender.send(
                SendMessageBuilder.builder()
                    .chatId(group.id())
                    .replyMessageId(groupEvent.messageId())
                    .text(text.apply(group))
                    .build()
            );
        });
    }
}
