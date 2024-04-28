package ru.homyakin.seeker.telegram.group.rumor;

import java.time.LocalDateTime;
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
                    groupService.updateNextRumorDate(group, nextRumorDate());
                },
                () -> logger.warn("No available rumors")
            );
    }

    private LocalDateTime nextRumorDate() {
        return TimeUtils.moscowTime().plus(
            RandomUtils.getRandomDuration(
                rumorConfig.minimalInterval(),
                rumorConfig.maximumInterval()
            )
        );
    }

    private SendMessage toMessage(Rumor rumor, Group group) {
        return SendMessageBuilder
            .builder()
            .chatId(group.id())
            .text(rumor.text(group.language()))
            .build();
    }
}
