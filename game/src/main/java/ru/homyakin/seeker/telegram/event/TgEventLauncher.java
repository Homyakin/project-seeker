package ru.homyakin.seeker.telegram.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.raid.RaidService;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.service.GroupEventService;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.game.group.action.UpdateGroupParameters;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.locale.raid.RaidLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.utils.TimeUtils;

import java.util.List;

@Service
public class TgEventLauncher {
    private static final Logger logger = LoggerFactory.getLogger(TgEventLauncher.class);
    private final GetGroup getGroup;
    private final UpdateGroupParameters updateGroupParameters;
    private final GroupTgService groupTgService;
    private final TelegramSender telegramSender;
    private final GroupEventService groupEventService;
    private final LockService lockService;
    private final RaidService raidService;

    public TgEventLauncher(
        GetGroup getGroup,
        UpdateGroupParameters updateGroupParameters,
        GroupTgService groupTgService,
        TelegramSender telegramSender,
        GroupEventService groupEventService,
        LockService lockService,
        RaidService raidService
    ) {
        this.getGroup = getGroup;
        this.updateGroupParameters = updateGroupParameters;
        this.groupTgService = groupTgService;
        this.telegramSender = telegramSender;
        this.groupEventService = groupEventService;
        this.lockService = lockService;
        this.raidService = raidService;
    }

    public void launchRaidsInGroups() {
        getGroup
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
        final var launchedRaidResult = raidService.launchRaid(raid, group.id(), group.raidLevel());
        final var launchedRaidEvent = launchedRaidResult.launchedRaidEvent();
        final var groupTg = groupTgService.forceGet(group.id());
        var result = telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(groupTg.id())
                .text(RaidLocalization.raidStarting(
                    groupTg.language(),
                    raid,
                    launchedRaidEvent,
                    List.of()
                ))
                .keyboard(InlineKeyboards.joinRaidKeyboard(
                    groupTg.language(),
                    launchedRaidEvent.id(),
                    launchedRaidResult.energyCost()
                ))
                .build()
        );
        if (result.isLeft()) {
            groupEventService.creationError(launchedRaidEvent.id());
            return;
        }
        groupEventService.createGroupEvent(launchedRaidEvent.id(), groupTg, result.get().getMessageId());
        updateGroupParameters.updateNextEventDate(
            group.id(),
            group.settings().eventDateInNextInterval().withOffsetSameInstant(TimeUtils.moscowOffset()).toLocalDateTime()
        );
    }
}
