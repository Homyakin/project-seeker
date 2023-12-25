package ru.homyakin.seeker.game.event.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.locale.raid.RaidLocalization;
import ru.homyakin.seeker.telegram.group.stats.GroupStatsService;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.game.event.config.EventConfig;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class EventManager {
    private static final Logger logger = LoggerFactory.getLogger(EventManager.class);
    private final EventConfig eventConfig;
    private final GroupService groupService;
    private final EventService eventService;
    private final TelegramSender telegramSender;
    private final LaunchedEventService launchedEventService;
    private final EventProcessing eventProcessing;
    private final GroupStatsService groupStatsService;
    private final LockService lockService;

    public EventManager(
        EventConfig eventConfig,
        GroupService groupService,
        EventService eventService,
        TelegramSender telegramSender,
        LaunchedEventService launchedEventService,
        EventProcessing eventProcessing,
        GroupStatsService groupStatsService,
        LockService lockService
    ) {
        this.eventConfig = eventConfig;
        this.groupService = groupService;
        this.eventService = eventService;
        this.telegramSender = telegramSender;
        this.launchedEventService = launchedEventService;
        this.eventProcessing = eventProcessing;
        this.groupStatsService = groupStatsService;
        this.lockService = lockService;
    }

    public void launchEventsInGroups() {
        groupService
            .getGetGroupsWithLessNextEventDate(TimeUtils.moscowTime())
            .stream()
            .filter(it -> it.activeTime().isActiveNow())
            .forEach(group -> {
                final var key = LockPrefixes.GROUP_EVENT.name() + group.id().value();
                lockService.tryLockAndExecute(
                    key,
                    () -> {
                        final var event = eventService.getRandomEvent();
                        launchEventInGroup(group, event);
                    }
                );
            });
    }

    public void stopEvents() {
        launchedEventService
            .getExpiredActiveEvents()
            .forEach(event -> {
                final var key = LockPrefixes.LAUNCHED_EVENT.name() + event.id();
                lockService.tryLockAndExecute(key, () -> stopLaunchedEvent(event));
            });
    }

    private void stopLaunchedEvent(LaunchedEvent launchedEvent) {
        logger.info("Stopping event " + launchedEvent.id());
        final var result = eventProcessing.processEvent(launchedEvent);

        launchedEventService.getGroupEvents(launchedEvent)
            .forEach(groupEvent -> {
                result.ifPresentOrElse(
                    raidResult -> {
                        launchedEventService.updateResult(launchedEvent, raidResult);
                        groupStatsService.updateRaidStats(groupEvent.groupId(), raidResult);
                    },
                    () -> launchedEventService.expireEvent(launchedEvent)
                );
                final var group = groupService.getOrCreate(groupEvent.groupId());
                final var event = eventService.getEventById(launchedEvent.eventId())
                    .orElseThrow(() -> new IllegalStateException("Can't end nonexistent event"));
                if (result.isEmpty()) {
                    telegramSender.send(EditMessageTextBuilder.builder()
                        .chatId(groupEvent.groupId())
                        .messageId(groupEvent.messageId())
                        .text(RaidLocalization.zeroParticipants(group.language()))
                        .build()
                    );
                } else {
                    final var participants = result.get().personageResults().stream()
                        .map(BattlePersonage::personage)
                        .toList();
                    telegramSender.send(EditMessageTextBuilder.builder()
                        .chatId(groupEvent.groupId())
                        .messageId(groupEvent.messageId())
                        .text(event.toEndMessage(group.language(), participants))
                        .build()
                    );
                    telegramSender.send(SendMessageBuilder.builder()
                        .chatId(groupEvent.groupId())
                        .text(event.endMessage(group.language(), result.get()))
                        .replyMessageId(groupEvent.messageId())
                        .build()
                    );
                }
            });

    }

    private void launchEventInGroup(Group group, Event event) {
        logger.info("Creating event " + event.id() + " for group " + group.id());
        final var launchedEvent = launchedEventService.createLaunchedEvent(event);
        var result = telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(group.id())
                .text(event.toStartMessage(group.language(), launchedEvent.startDate(), launchedEvent.endDate()))
                .keyboard(InlineKeyboards.joinRaidEventKeyboard(group.language(), launchedEvent.id()))
                .build()
        );
        if (result.isLeft()) {
            launchedEventService.creationError(launchedEvent);
            return;
        }
        launchedEventService.addGroupMessage(launchedEvent, group, result.get().getMessageId());
        groupService.updateNextEventDate(
            group,
            TimeUtils.moscowTime()
                .plus(
                    RandomUtils.getRandomDuration(
                        eventConfig.minimalInterval(), eventConfig.maximumInterval()
                    )
                )
        );
    }
}
