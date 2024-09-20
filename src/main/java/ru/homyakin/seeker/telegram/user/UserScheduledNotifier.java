package ru.homyakin.seeker.telegram.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class UserScheduledNotifier {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final UserService userService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public UserScheduledNotifier(UserService userService, PersonageService personageService, TelegramSender telegramSender) {
        this.userService = userService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Scheduled(cron = "0 * * * * *")
    public void notifyUsersAboutEnergyRegen() {
        for (final var user : userService.getUsersWithRecoveredEnergy()) {
            if (user.isActivePrivateMessages()) {
                logger.info("Notify user {} about energy regen", user.id());
                telegramSender.send(
                    SendMessageBuilder
                        .builder()
                        .chatId(user.id())
                        .text(CommonLocalization.energyRecovered(user.language()))
                        .build()
                ) // Тут сайд эффект, энергия автоматом регенерируется при получении персонажа
                    .peek(_ -> personageService.getByIdForce(user.personageId()));
            } else {
                logger.info("Skip notify user {} about energy regen, disabled in private", user.id());
                personageService.getByIdForce(user.personageId());
            }
        }
    }
}
