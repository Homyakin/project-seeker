package ru.homyakin.seeker.telegram.group.rumor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.homyakin.seeker.game.rumor.Rumor;
import ru.homyakin.seeker.game.rumor.RumorConfig;
import ru.homyakin.seeker.game.rumor.RumorService;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.TimeUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class RumorTgService {
    private static final Logger logger = LoggerFactory.getLogger(RumorTgService.class);
    private final GroupService groupService;
    private final RumorService rumorService;
    private final TelegramSender telegramSender;
    private final RumorConfig rumorConfig;
    private final LockService lockService;

    public RumorTgService(
        GroupService groupService,
        RumorService rumorService,
        TelegramSender telegramSender,
        RumorConfig rumorConfig,
        LockService lockService
    ) {
        this.groupService = groupService;
        this.rumorService = rumorService;
        this.telegramSender = telegramSender;
        this.rumorConfig = rumorConfig;
        this.lockService = lockService;
    }

    @Scheduled(cron = "0 * * * * *")
    public void createNewRumors() {
        logger.debug("Creating new rumors");
        groupService.getGetGroupsWithLessNextRumorDate(TimeUtils.moscowTime())
            .stream()
            .filter(group -> group.isActive() && group.activeTime().isActiveNow())
            .forEach(
                group -> lockService.tryLockAndExecute(
                    LockPrefixes.RUMOR.name() + group.id().value(),
                    () -> launchRandomRumorInGroup(group)
                )
            );
    }

    private void launchRandomRumorInGroup(Group group) {
        rumorService.getRandomAvailableRumor()
            .ifPresentOrElse(
                rumor -> {
                    logger.info("Launching rumor " + rumor.code() + " in group " + group.id());
                    telegramSender.send(toMessage(rumor, group));
                    groupService.updateNextRumorDate(group, nextRumorDate(group));
                },
                () -> logger.warn("No available rumors")
            );
    }

    private LocalDateTime nextRumorDate(Group group) {
        final var time = TimeUtils.moscowTime()
            .plus(
                RandomUtils.getRandomDuration(
                    rumorConfig.minimalInterval(), rumorConfig.maximumInterval()
                )
            );

        if (group.activeTime().isHourInInterval(time.getHour())) {
            return time;
        } else {
            return time.plus(Duration.of(1, ChronoUnit.DAYS))
                .withHour(RandomUtils.getInInterval(group.activeTime().startHour(), group.activeTime().endHour() - 1))
                .withMinute(RandomUtils.getInInterval(0, 60));
        }
    }

    private SendMessage toMessage(Rumor rumor, Group group) {
        return SendMessageBuilder
            .builder()
            .chatId(group.id())
            .text(rumor.text(group.language()))
            .build();
    }
}
