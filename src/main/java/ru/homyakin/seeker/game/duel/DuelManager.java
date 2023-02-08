package ru.homyakin.seeker.game.duel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class DuelManager {
    private static final Logger logger = LoggerFactory.getLogger(DuelManager.class);
    private final DuelService duelService;
    private final GroupService groupService;
    private final TelegramSender telegramSender;

    public DuelManager(
        DuelService duelService,
        GroupService groupService,
        TelegramSender telegramSender
    ) {
        this.duelService = duelService;
        this.groupService = groupService;
        this.telegramSender = telegramSender;
    }

    @Scheduled(cron = "0 * * * * *")
    public void scheduledExpireWaitingDuels() {
        duelService.getExpiringDuels()
            .forEach(duel -> {
                    logger.debug("Expiring duel " + duel.id());
                    duelService.expireDuel(duel.id());
                    if (duel.messageId().isPresent()) {
                        final var group = groupService.getOrCreate(duel.groupId());
                        telegramSender.send(
                            TelegramMethods.createEditMessageText(
                                group.id(),
                                duel.messageId().get(),
                                DuelLocalization.get(group.language()).expiredDuel()
                            )
                        );
                    } else {
                        logger.warn("No message for duel " + duel.id());
                    }
                }
            );
    }
}
