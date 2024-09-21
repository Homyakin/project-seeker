package ru.homyakin.seeker.telegram.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.raid.RaidService;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.service.EventProcessing;
import ru.homyakin.seeker.game.event.service.LaunchedEventService;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.locale.personal.PersonalQuestLocalization;
import ru.homyakin.seeker.telegram.group.stats.GroupStatsService;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class EventManager {
    private static final Logger logger = LoggerFactory.getLogger(EventManager.class);
    private final GroupService groupService;
    private final TelegramSender telegramSender;
    private final LaunchedEventService launchedEventService;
    private final EventProcessing eventProcessing;
    private final GroupStatsService groupStatsService;
    private final LockService lockService;
    private final RaidService raidService;
    private final UserService userService;

    public EventManager(
        GroupService groupService,
        TelegramSender telegramSender,
        LaunchedEventService launchedEventService,
        EventProcessing eventProcessing,
        GroupStatsService groupStatsService,
        LockService lockService,
        RaidService raidService, UserService userService
    ) {
        this.groupService = groupService;
        this.telegramSender = telegramSender;
        this.launchedEventService = launchedEventService;
        this.eventProcessing = eventProcessing;
        this.groupStatsService = groupStatsService;
        this.lockService = lockService;
        this.raidService = raidService;
        this.userService = userService;
    }

    public void launchEventsInGroups() {
        groupService
            .getGetGroupsWithLessNextEventDate(TimeUtils.moscowTime())
            .stream()
            .filter(it -> it.settings().isActiveForEventNow())
            .forEach(group -> {
                final var key = LockPrefixes.GROUP_EVENT.name() + group.id().value();
                lockService.tryLockAndExecute(
                    key,
                    () -> raidService.getRandomRaid()
                        .ifPresentOrElse(
                            event -> launchRaidInGroup(group, event),
                            () -> logger.warn("No raids in database")
                        )
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
        switch (result) {
            case EventResult.Raid raidResult -> stopRaid(launchedEvent, raidResult);
            case EventResult.PersonalQuestResult personalQuestResult -> processPersonalQuestResult(personalQuestResult);
        }
    }

    private void processPersonalQuestResult(EventResult.PersonalQuestResult result) {
        final var message = SendMessageBuilder.builder();
        switch (result) {
            case EventResult.PersonalQuestResult.Error _ -> {
                return;
            }
            case EventResult.PersonalQuestResult.Failure failure -> {
                final var user = userService.getByPersonageIdForce(failure.personage().id());
                message.chatId(user.id());
                message.text(PersonalQuestLocalization.failedQuest(user.language(), failure));
            }
            case EventResult.PersonalQuestResult.Success success -> {
                final var user = userService.getByPersonageIdForce(success.personage().id());
                message.chatId(user.id());
                message.text(PersonalQuestLocalization.successQuest(user.language(), success));
            }
        }
        telegramSender.send(message.build());
    }

    private void stopRaid(LaunchedEvent launchedEvent, EventResult.Raid result) {
        launchedEventService.getGroupEvents(launchedEvent)
            .forEach(
                groupEvent -> {
                    launchedEventService.updateResult(launchedEvent, result);
                    groupStatsService.updateRaidStats(groupEvent.groupId(), result);
                    final var group = groupService.getOrCreate(groupEvent.groupId());
                    final var event = raidService.getByEventId(launchedEvent.eventId())
                        .orElseThrow(() -> new IllegalStateException("Can't end nonexistent event"));
                    telegramSender.send(EditMessageTextBuilder.builder()
                        .chatId(groupEvent.groupId())
                        .messageId(groupEvent.messageId())
                        .text(event.toEndMessage(result, group.language()))
                        .build()
                    );
                    if (!result.isExpired()) {
                        telegramSender.send(SendMessageBuilder.builder()
                            .chatId(groupEvent.groupId())
                            .text(event.endMessage(group.language(), result))
                            .replyMessageId(groupEvent.messageId())
                            .build()
                        );
                    }
                }
            );
    }

    private void launchRaidInGroup(Group group, Raid raid) {
        logger.info("Creating raid " + raid.code() + " for group " + group.id());
        final var launchedEvent = launchedEventService.createLaunchedEventFromRaid(raid, TimeUtils.moscowTime());
        var result = telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(group.id())
                .text(raid.toStartMessage(group.language(), launchedEvent.startDate(), launchedEvent.endDate()))
                .keyboard(InlineKeyboards.joinRaidKeyboard(group.language(), launchedEvent.id()))
                .build()
        );
        if (result.isLeft()) {
            launchedEventService.creationError(launchedEvent);
            return;
        }
        launchedEventService.addGroupMessage(launchedEvent, group, result.get().getMessageId());
        groupService.updateNextEventDate(
            group,
            group.settings().eventDateInNextInterval().withOffsetSameInstant(TimeUtils.moscowOffset()).toLocalDateTime()
        );
    }
}
