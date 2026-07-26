package ru.homyakin.seeker.telegram.anomaly;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.battle.BattleVisualizerConfig;
import ru.homyakin.seeker.game.event.anomaly.action.AnomalyService;
import ru.homyakin.seeker.game.event.anomaly.entity.Anomaly;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyConfig;
import ru.homyakin.seeker.game.event.anomaly.entity.SendAnomalyChallengeToGroup;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.models.GroupLaunchedEvent;
import ru.homyakin.seeker.game.event.service.GroupEventService;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.anomaly.AnomalyLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.utils.AnomalyKeyboards;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.OutpostKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class TelegramAnomalyService implements SendAnomalyChallengeToGroup {
    private final GroupTgService groupTgService;
    private final GetGroup getGroup;
    private final TelegramSender telegramSender;
    private final GroupEventService groupEventService;
    private final LaunchedEventService launchedEventService;
    private final AnomalyService anomalyService;
    private final AnomalyConfig anomalyConfig;
    private final BattleVisualizerConfig battleVisualizerConfig;

    public TelegramAnomalyService(
        GroupTgService groupTgService,
        GetGroup getGroup,
        TelegramSender telegramSender,
        GroupEventService groupEventService,
        LaunchedEventService launchedEventService,
        AnomalyService anomalyService,
        AnomalyConfig anomalyConfig,
        BattleVisualizerConfig battleVisualizerConfig
    ) {
        this.groupTgService = groupTgService;
        this.getGroup = getGroup;
        this.telegramSender = telegramSender;
        this.groupEventService = groupEventService;
        this.launchedEventService = launchedEventService;
        this.anomalyService = anomalyService;
        this.anomalyConfig = anomalyConfig;
        this.battleVisualizerConfig = battleVisualizerConfig;
    }

    @Override
    public void send(GroupId defenderGroupId, LaunchedEvent event) {
        if (!getGroup.forceGet(defenderGroupId).isActive()) {
            return;
        }
        final var anomaly = anomalyService.findAnomaly(event.id()).orElseThrow();
        if (!(anomaly instanceof Anomaly.Dangerous.Challenged)
            && !(anomaly instanceof Anomaly.Dangerous.Accepted)) {
            throw new IllegalStateException("Expected defense anomaly for event " + event.id());
        }
        final var groupTg = groupTgService.forceGet(defenderGroupId);
        final var participants = anomalyService.participants(event.id()).list().stream()
            .filter(participant -> participant.personage().memberGroupId()
                .filter(defenderGroupId::equals)
                .isPresent())
            .toList();
        final var result = telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(groupTg.id())
                .text(AnomalyLocalization.challenge(
                    groupTg.language(),
                    anomaly,
                    participants,
                    anomalyConfig.partySize()
                ))
                .keyboard(AnomalyKeyboards.forEvent(
                    groupTg.language(), event.id(), anomaly, defenderGroupId
                ))
                .build()
        );
        result.peek(message ->
            groupEventService.createGroupEvent(event.id(), groupTg, message.getMessageId())
        );
    }

    public String eventText(
        Language language,
        LaunchedEvent event,
        Anomaly anomaly,
        GroupId viewerGroupId
    ) {
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
                    event,
                    searching.ownerPersonageId()
                );
            case Anomaly.Dangerous.Challenged challenged -> {
                if (viewerGroupId.equals(challenged.opponentGroupId())) {
                    yield AnomalyLocalization.challenge(
                        language,
                        challenged,
                        participantsOfGroup(participants, challenged.opponentGroupId()),
                        anomalyConfig.partySize()
                    );
                }
                yield AnomalyLocalization.searching(
                    language,
                    participantsOfGroup(participants, challenged.groupId()),
                    anomalyConfig.partySize(),
                    event,
                    challenged.ownerPersonageId()
                );
            }
            case Anomaly.Dangerous.Accepted accepted -> {
                if (viewerGroupId.equals(accepted.opponentGroupId())) {
                    yield AnomalyLocalization.challenge(
                        language,
                        accepted,
                        participantsOfGroup(participants, accepted.opponentGroupId()),
                        anomalyConfig.partySize()
                    );
                }
                yield AnomalyLocalization.searching(
                    language,
                    participantsOfGroup(participants, accepted.groupId()),
                    anomalyConfig.partySize(),
                    event,
                    accepted.ownerPersonageId()
                );
            }
            case Anomaly.Safe _ -> AnomalyLocalization.expired(language);
        };
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
            case EventResult.AnomalyResult.NoMatch noMatch ->
                replyToGroupEvents(
                    noMatch.launchedEventId(),
                    group -> AnomalyLocalization.noMatch(group.language(), anomalyConfig.noMatchReward())
                );
            case EventResult.AnomalyResult.ChallengeTimedOut timedOut -> {
                clearEventKeyboardsForDomainGroup(
                    timedOut.launchedEventId(),
                    timedOut.opponentGroupId()
                );
                replyToGroupEventsForDomainGroup(
                    timedOut.launchedEventId(),
                    timedOut.opponentGroupId(),
                    group -> AnomalyLocalization.expired(group.language())
                );
            }
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
        clearEventKeyboards(result.launchedEventId());
        groupEventService.getByLaunchedEventId(result.launchedEventId()).forEach(groupEvent -> {
            final var group = groupTgService.getOrCreate(groupEvent.groupId());
            final boolean victory = group.domainGroupId().equals(result.winnerGroupId());
            final var reward = victory ? anomalyConfig.victoryReward() : anomalyConfig.defeatReward();
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
        clearEventKeyboards(result.launchedEventId());
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

    private void clearEventKeyboards(long launchedEventId) {
        final var event = launchedEventService.getById(launchedEventId).orElse(null);
        final var anomaly = anomalyService.findAnomaly(launchedEventId).orElse(null);
        if (event == null || anomaly == null) {
            return;
        }
        groupEventService.getByLaunchedEventId(launchedEventId).forEach(groupEvent ->
            clearEventKeyboard(groupEvent, event, anomaly)
        );
    }

    private void clearEventKeyboardsForDomainGroup(long launchedEventId, GroupId domainGroupId) {
        final var event = launchedEventService.getById(launchedEventId).orElse(null);
        final var anomaly = anomalyService.findAnomaly(launchedEventId).orElse(null);
        if (event == null || anomaly == null) {
            return;
        }
        groupEventService.getByLaunchedEventId(launchedEventId).forEach(groupEvent -> {
            final var group = groupTgService.getOrCreate(groupEvent.groupId());
            if (!group.domainGroupId().equals(domainGroupId)) {
                return;
            }
            clearEventKeyboard(groupEvent, event, anomaly);
        });
    }

    private void clearEventKeyboard(
        GroupLaunchedEvent groupEvent,
        LaunchedEvent event,
        Anomaly anomaly
    ) {
        final var group = groupTgService.getOrCreate(groupEvent.groupId());
        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(group.id())
                .messageId(groupEvent.messageId())
                .text(eventText(group.language(), event, anomaly, group.domainGroupId()))
                .keyboard(OutpostKeyboards.emptyInlineKeyboard())
                .build()
        );
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

    private void replyToGroupEventsForDomainGroup(
        long launchedEventId,
        GroupId domainGroupId,
        java.util.function.Function<GroupTg, String> text
    ) {
        groupEventService.getByLaunchedEventId(launchedEventId).forEach(groupEvent -> {
            final var group = groupTgService.getOrCreate(groupEvent.groupId());
            if (!group.domainGroupId().equals(domainGroupId)) {
                return;
            }
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
