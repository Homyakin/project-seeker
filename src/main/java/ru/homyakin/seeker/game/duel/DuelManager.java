package ru.homyakin.seeker.game.duel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;

@Component
public class DuelManager {
    private static final Logger logger = LoggerFactory.getLogger(DuelManager.class);
    private final DuelService duelService;
    private final GroupService groupService;
    private final TelegramSender telegramSender;
    private final PersonageService personageService;
    private final UserService userService;

    public DuelManager(
        DuelService duelService,
        GroupService groupService,
        TelegramSender telegramSender,
        PersonageService personageService,
        UserService userService
    ) {
        this.duelService = duelService;
        this.groupService = groupService;
        this.telegramSender = telegramSender;
        this.personageService = personageService;
        this.userService = userService;
    }

    @Scheduled(cron = "0 * * * * *")
    public void scheduledExpireWaitingDuels() {
        duelService.getExpiringDuels()
            .forEach(duel -> {
                    logger.debug("Expiring duel " + duel.id());
                    duelService.expireDuel(duel.id());
                    if (duel.messageId().isPresent()) {
                        final var group = groupService.getOrCreate(duel.groupId());
                        final var acceptor = personageService.getByIdForce(duel.acceptingPersonageId());
                        final var user = userService.getByPersonageIdForce(acceptor.id());
                        telegramSender.send(EditMessageTextBuilder.builder()
                            .chatId(group.id())
                            .messageId(duel.messageId().get())
                            .text(DuelLocalization.expiredDuel(group.language(), TgPersonageMention.of(acceptor, user.id())))
                            .build()
                        );
                    } else {
                        logger.warn("No message for duel " + duel.id());
                    }
                }
            );
    }
}
