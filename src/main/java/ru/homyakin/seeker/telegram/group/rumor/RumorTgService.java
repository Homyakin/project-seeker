package ru.homyakin.seeker.telegram.group.rumor;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.game.group.action.UpdateGroupParameters;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.rumor.Rumor;
import ru.homyakin.seeker.game.rumor.RumorConfig;
import ru.homyakin.seeker.game.rumor.RumorService;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class RumorTgService {
    private static final Logger logger = LoggerFactory.getLogger(RumorTgService.class);
    private final GetGroup getGroup;
    private final UpdateGroupParameters updateGroupParameters;
    private final GroupTgService groupTgService;
    private final RumorService rumorService;
    private final TelegramSender telegramSender;
    private final RumorConfig rumorConfig;
    private final LockService lockService;

    public RumorTgService(
        GetGroup getGroup, UpdateGroupParameters updateGroupParameters, GroupTgService groupTgService,
        RumorService rumorService,
        TelegramSender telegramSender,
        RumorConfig rumorConfig,
        LockService lockService
    ) {
        this.getGroup = getGroup;
        this.updateGroupParameters = updateGroupParameters;
        this.groupTgService = groupTgService;
        this.rumorService = rumorService;
        this.telegramSender = telegramSender;
        this.rumorConfig = rumorConfig;
        this.lockService = lockService;
    }

    @Scheduled(cron = "0 * * * * *")
    public void createNewRumors() {
        logger.debug("Creating new rumors");
        getGroup.getGetGroupsWithLessNextRumorDate(TimeUtils.moscowTime())
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
                    final var groupTg = groupTgService.forceGet(group.id());
                    logger.info("Launching rumor " + rumor.code() + " in group " + group.id());
                    telegramSender.send(toMessage(rumor, groupTg));
                    updateGroupParameters.updateNextRumorDate(group.id(), nextRumorDate());
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

    private SendMessage toMessage(Rumor rumor, GroupTg group) {
        return SendMessageBuilder
            .builder()
            .chatId(group.id())
            .text(rumor.text(group.language()))
            .build();
    }
}
