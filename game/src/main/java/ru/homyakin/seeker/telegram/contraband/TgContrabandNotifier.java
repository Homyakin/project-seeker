package ru.homyakin.seeker.telegram.contraband;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.contraband.entity.Contraband;
import ru.homyakin.seeker.game.contraband.entity.ContrabandConfig;
import ru.homyakin.seeker.game.contraband.entity.ContrabandOpenResult;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.notification.entity.Notification;
import ru.homyakin.seeker.telegram.personage.TgPersonageNotificationService;

@Service
public class TgContrabandNotifier {
    private final TgPersonageNotificationService notificationService;
    private final PersonageService personageService;
    private final ContrabandConfig config;

    public TgContrabandNotifier(
        TgPersonageNotificationService notificationService,
        PersonageService personageService,
        ContrabandConfig config
    ) {
        this.notificationService = notificationService;
        this.personageService = personageService;
        this.config = config;
    }

    public void sendContrabandFoundToFinder(Contraband contraband) {
        notificationService.sendNotification(
            contraband.finderPersonageId(),
            new Notification.ContrabandFound(
                contraband,
                config.finderSuccessChancePercent(),
                config.sellPrice(contraband.tier())
            )
        );
    }

    public void sendContrabandToReceiver(Contraband contraband) {
        final var finder = personageService.getByIdForce(contraband.finderPersonageId());
        notificationService.sendNotification(
            contraband.receiverPersonageId().orElseThrow(),
            new Notification.ContrabandReceived(
                contraband,
                finder,
                config.receiverSuccessChancePercent()
            )
        );
    }

    public void sendEchoToFinder(Contraband contraband, ContrabandOpenResult result) {
        final var receiver = personageService.getByIdForce(contraband.receiverPersonageId().orElseThrow());
        final var notification = switch (result) {
            case ContrabandOpenResult.Success _ ->
                new Notification.ContrabandEchoSuccess(contraband, receiver);
            case ContrabandOpenResult.Failure _ ->
                new Notification.ContrabandEchoFailure(contraband, receiver);
        };
        notificationService.sendNotification(contraband.finderPersonageId(), notification);
    }

    public void sendContrabandExpired(Contraband contraband) {
        final var notification = new Notification.ContrabandExpired(contraband.tier());
        if (contraband.canBeProcessedByFinder()) {
            notificationService.sendNotification(contraband.finderPersonageId(), notification);
        } else if (contraband.canBeProcessedByReceiver()) {
            notificationService.sendNotification(contraband.receiverPersonageId().orElseThrow(), notification);
        }
    }
}
