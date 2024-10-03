package ru.homyakin.seeker.telegram.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.raid.RaidService;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.service.GroupEventService;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class TgEventLauncher {
    private static final Logger logger = LoggerFactory.getLogger(TgEventLauncher.class);
    private final GroupService groupService;
    private final TelegramSender telegramSender;
    private final GroupEventService groupEventService;
    private final LockService lockService;
    private final RaidService raidService;

    public TgEventLauncher(
        GroupService groupService,
        TelegramSender telegramSender,
        GroupEventService groupEventService,
        LockService lockService,
        RaidService raidService
    ) {
        this.groupService = groupService;
        this.telegramSender = telegramSender;
        this.groupEventService = groupEventService;
        this.lockService = lockService;
        this.raidService = raidService;
    }

    public void launchRaidsInGroups() {
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

    private void launchRaidInGroup(Group group, Raid raid) {
        logger.info("Creating raid " + raid.code() + " for group " + group.id());
        final var launchedRaidResult = raidService.launchRaid(raid);
        final var launchedEvent = launchedRaidResult.launchedEvent();
        var result = telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(group.id())
                .text(raid.toStartMessage(group.language(), launchedEvent.startDate(), launchedEvent.endDate()))
                .keyboard(InlineKeyboards.joinRaidKeyboard(group.language(), launchedEvent.id(), launchedRaidResult.energyCost()))
                .build()
        );
        if (result.isLeft()) {
            groupEventService.creationError(launchedEvent);
            return;
        }
        groupEventService.createGroupEvent(launchedEvent, group, result.get().getMessageId());
        groupService.updateNextEventDate(
            group,
            group.settings().eventDateInNextInterval().withOffsetSameInstant(TimeUtils.moscowOffset()).toLocalDateTime()
        );
    }
}
