package ru.homyakin.seeker.game.event.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.telegram.group.GroupStatsService;
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

    public EventManager(
        EventConfig eventConfig,
        GroupService groupService,
        EventService eventService,
        TelegramSender telegramSender,
        LaunchedEventService launchedEventService,
        EventProcessing eventProcessing,
        GroupStatsService groupStatsService) {
        this.eventConfig = eventConfig;
        this.groupService = groupService;
        this.eventService = eventService;
        this.telegramSender = telegramSender;
        this.launchedEventService = launchedEventService;
        this.eventProcessing = eventProcessing;
        this.groupStatsService = groupStatsService;
    }

    public void launchEventsInGroups() {
        //TODO Здесь может возникнуть какая-нибудь многопоточная гонка, потом можно добавить локи на чаты
        groupService
            .getGetGroupsWithLessNextEventDate(TimeUtils.moscowTime())
            .forEach(group -> {
                logger.debug("Creating event for group " + group.id());
                final var event = eventService.getRandomEvent();
                launchEventInGroup(group, event);
            });
    }

    public void stopEvents() {
        launchedEventService
            .getExpiredActiveEvents()
            .forEach(this::stopLaunchedEvent);
    }

    private void stopLaunchedEvent(LaunchedEvent launchedEvent) {
        logger.debug("Stopping event " + launchedEvent.id());
        final var result = eventProcessing.processEvent(launchedEvent);
        launchedEventService.updateActive(launchedEvent, false);
        launchedEventService.getGroupEvents(launchedEvent)
            .forEach(groupEvent -> {
                if (result instanceof EventResult.Success) {
                    groupStatsService.increaseRaidsComplete(groupEvent.groupId(), 1);
                }
                final var group = groupService.getOrCreate(groupEvent.groupId());
                final var event = eventService.getEventById(launchedEvent.eventId())
                    .orElseThrow(() -> new IllegalStateException("Can't end nonexistent event"));
                telegramSender.send(EditMessageTextBuilder.builder()
                    .chatId(groupEvent.groupId())
                    .messageId(groupEvent.messageId())
                    .text(event.toStartMessage(group.language()))
                    .build()
                );
                telegramSender.send(SendMessageBuilder.builder()
                    .chatId(groupEvent.groupId())
                    .text(event.endMessage(group.language(), result))
                    .replyMessageId(groupEvent.messageId())
                    .build()
                );
            });

    }

    private void launchEventInGroup(Group group, Event event) {
        final var launchedEvent = launchedEventService.createLaunchedEvent(event);
        var result = telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(group.id())
                .text(event.toStartMessage(group.language(), launchedEvent.endDate()))
                .keyboard(InlineKeyboards.joinRaidEventKeyboard(group.language(), launchedEvent.id()))
                .build()
        );
        if (result.isLeft()) {
            launchedEventService.updateActive(launchedEvent, false);
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
