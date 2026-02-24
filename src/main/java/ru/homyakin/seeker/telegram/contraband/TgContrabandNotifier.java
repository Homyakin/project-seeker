package ru.homyakin.seeker.telegram.contraband;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.contraband.entity.Contraband;
import ru.homyakin.seeker.game.contraband.entity.ContrabandConfig;
import ru.homyakin.seeker.game.contraband.entity.ContrabandOpenResult;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.contraband.ContrabandLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.ContrabandKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Service
public class TgContrabandNotifier {
    private static final Logger logger = LoggerFactory.getLogger(TgContrabandNotifier.class);
    private final TelegramSender telegramSender;
    private final UserService userService;
    private final PersonageService personageService;
    private final ContrabandConfig config;

    public TgContrabandNotifier(
        TelegramSender telegramSender,
        UserService userService,
        PersonageService personageService,
        ContrabandConfig config
    ) {
        this.telegramSender = telegramSender;
        this.userService = userService;
        this.personageService = personageService;
        this.config = config;
    }

    public void sendContrabandFoundToFinder(Contraband contraband) {
        final var user = userService.getByPersonageIdForce(contraband.finderPersonageId());
        if (!user.isActivePrivateMessages()) {
            logger.info("Skip contraband notification for user {}, disabled private messages", user.id());
            return;
        }
        final var language = user.language();
        final var text = ContrabandLocalization.contrabandFoundPrivateMessage(
            language, contraband, config.finderSuccessChancePercent(), config.sellPrice(contraband.tier())
        );
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .keyboard(ContrabandKeyboards.finderChoiceKeyboard(language, contraband, config))
            .build()
        );
    }

    public void sendContrabandToReceiver(Contraband contraband) {
        final var receiverUser = userService.getByPersonageIdForce(
            contraband.receiverPersonageId().orElseThrow()
        );
        if (!receiverUser.isActivePrivateMessages()) {
            logger.info("Skip contraband notification for receiver {}, disabled private messages", receiverUser.id());
            return;
        }
        final var finder = personageService.getByIdForce(contraband.finderPersonageId());
        final var language = receiverUser.language();
        final var text = ContrabandLocalization.receiverNotification(
            language, contraband, finder, config.receiverSuccessChancePercent()
        );
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(receiverUser.id())
            .text(text)
            .keyboard(ContrabandKeyboards.receiverOpenKeyboard(language, contraband, config))
            .build()
        );
    }

    public void sendEchoToFinder(Contraband contraband, ContrabandOpenResult result) {
        final var finderUser = userService.getByPersonageIdForce(contraband.finderPersonageId());
        if (!finderUser.isActivePrivateMessages()) {
            return;
        }
        final var language = finderUser.language();
        final var receiver = personageService.getByIdForce(contraband.receiverPersonageId().orElseThrow());
        final String text = switch (result) {
            case ContrabandOpenResult.Success _ ->
                ContrabandLocalization.echoToFinderSuccess(language, contraband, receiver);
            case ContrabandOpenResult.Failure _ ->
                ContrabandLocalization.echoToFinderFailure(language, contraband, receiver);
        };
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(finderUser.id())
            .text(text)
            .build()
        );
    }

    public void sendContrabandExpired(Contraband contraband) {
        if (contraband.canBeProcessedByFinder()) {
            final var finderUser = userService.getByPersonageIdForce(contraband.finderPersonageId());
            if (finderUser.isActivePrivateMessages()) {
                telegramSender.send(SendMessageBuilder.builder()
                    .chatId(finderUser.id())
                    .text(ContrabandLocalization.contrabandExpired(finderUser.language(), contraband.tier()))
                    .build()
                );
            }
        } else if (contraband.canBeProcessedByReceiver()) {
            final var receiverUser = userService.getByPersonageIdForce(
                contraband.receiverPersonageId().orElseThrow()
            );
            if (receiverUser.isActivePrivateMessages()) {
                telegramSender.send(SendMessageBuilder.builder()
                    .chatId(receiverUser.id())
                    .text(ContrabandLocalization.contrabandExpired(receiverUser.language(), contraband.tier()))
                    .build()
                );
            }
        }
    }
}
