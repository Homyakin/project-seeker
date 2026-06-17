package ru.homyakin.seeker.telegram.contraband;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.contraband.action.ContrabandService;

@Component
public class ContrabandScheduler {
    private static final Logger logger = LoggerFactory.getLogger(ContrabandScheduler.class);
    private final ContrabandService contrabandService;
    private final TgContrabandNotifier contrabandNotifier;

    public ContrabandScheduler(
        ContrabandService contrabandService,
        TgContrabandNotifier contrabandNotifier
    ) {
        this.contrabandService = contrabandService;
        this.contrabandNotifier = contrabandNotifier;
    }

    @Scheduled(fixedRate = 60_000)
    public void processBlackMarket() {
        final var pending = contrabandService.findPendingForBlackMarket();
        for (final var contraband : pending) {
            try {
                final var assigned = contrabandService.assignReceiver(contraband);
                assigned.ifPresent(contrabandNotifier::sendContrabandToReceiver);
            } catch (Exception e) {
                logger.error("Error processing black market contraband {}", contraband.id(), e);
            }
        }
    }

    @Scheduled(fixedRate = 60_000)
    public void expireContrabands() {
        final var expired = contrabandService.findExpired(java.time.LocalDateTime.now());
        for (final var contraband : expired) {
            try {
                contrabandService.expire(contraband);
                contrabandNotifier.sendContrabandExpired(contraband);
            } catch (Exception e) {
                logger.error("Error expiring contraband {}", contraband.id(), e);
            }
        }
    }
}
