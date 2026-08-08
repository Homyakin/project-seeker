package ru.homyakin.seeker.telegram.anomaly;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.battle.BattleVisualizerConfig;
import ru.homyakin.seeker.game.event.anomaly.action.AnomalyService;
import ru.homyakin.seeker.game.event.anomaly.entity.Anomaly;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyConfig;
import ru.homyakin.seeker.game.event.anomaly.entity.NotifyAnomalyBattleFinished;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.service.GroupEventService;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.anomaly.AnomalyLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class TelegramAnomalyService implements NotifyAnomalyBattleFinished {
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
            case Anomaly.Dangerous.Gathering gathering ->
                AnomalyLocalization.gathering(
                    language, gathering, participants, anomalyConfig.partySize(), event
                );
            case Anomaly.Dangerous.Searching searching ->
                AnomalyLocalization.searching(
                    language,
                    participantsOfGroup(participants, searching.groupId()),
                    anomalyConfig.partySize(),
                    anomalyConfig.dangerousSearchDuration(),
                    searching.ownerPersonageId()
                );
            case Anomaly.Dangerous.Accepted _ -> AnomalyLocalization.expired(language);
            case Anomaly.Safe _ -> AnomalyLocalization.expired(language);
        };
    }

    @Override
    public void notify(EventResult.AnomalyResult.BattleFinished result) {
        notifyBattleFinished(result);
    }

    private static java.util.List<ru.homyakin.seeker.game.personage.event.EventParticipant> participantsOfGroup(
        java.util.List<ru.homyakin.seeker.game.personage.event.EventParticipant> participants,
        GroupId groupId
    ) {
        return participants.stream()
            .filter(participant -> participant.personage().memberGroupId()
                .filter(groupId::equals)
                .isPresent())
            .toList();
    }

    public void notifyExpired(LaunchedEvent event, EventResult.AnomalyResult result) {
        switch (result) {
            case EventResult.AnomalyResult.PveBattleFinished pve ->
                notifyPveBattleFinished(pve);
            case EventResult.AnomalyResult.BattleFinished battle ->
                notifyBattleFinished(battle);
            case EventResult.AnomalyResult.ExpiredGathering _,
                 EventResult.AnomalyResult.AlreadyFinal _ ->
                replyToGroupEvents(
                    event.id(),
                    group -> AnomalyLocalization.expired(group.language())
                );
        }
    }

    public void notifyBattleFinished(EventResult.AnomalyResult.BattleFinished result) {
        final var link = battleVisualizerConfig.battleUrl(result.launchedEventId());
        final var winnerGroup = getGroup.forceGet(result.winnerGroupId());
        final var loserGroup = getGroup.forceGet(result.loserGroupId());
        groupEventService.getByLaunchedEventId(result.launchedEventId()).forEach(groupEvent -> {
            final var group = groupTgService.getOrCreate(groupEvent.groupId());
            final boolean victory = group.domainGroupId().equals(result.winnerGroupId());
            final var reward = victory ? anomalyConfig.gvgWinReward() : anomalyConfig.gvgLoseReward();
            final var text = AnomalyLocalization.battleResult(
                group.language(),
                winnerGroup,
                loserGroup,
                result.winnerResults(),
                result.loserResults(),
                reward
            );
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

    private void notifyPveBattleFinished(EventResult.AnomalyResult.PveBattleFinished result) {
        final var link = battleVisualizerConfig.battleUrl(result.launchedEventId());
        groupEventService.getByLaunchedEventId(result.launchedEventId()).forEach(groupEvent -> {
            final var group = groupTgService.getOrCreate(groupEvent.groupId());
            if (!group.domainGroupId().equals(result.initiatorGroupId())) {
                return;
            }
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
