package ru.homyakin.seeker.game.personage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.notification.action.FullEnergyNotificationCommand;

@Component
public class RegenEnergyScheduler {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PersonageService personageService;
    private final PersonageDao personageDao;
    private final FullEnergyNotificationCommand fullEnergyNotificationCommand;

    public RegenEnergyScheduler(
        PersonageService personageService,
        PersonageDao personageDao,
        FullEnergyNotificationCommand fullEnergyNotificationCommand
    ) {
        this.personageService = personageService;
        this.personageDao = personageDao;
        this.fullEnergyNotificationCommand = fullEnergyNotificationCommand;
    }

    @Scheduled(cron = "0 * * * * *")
    public void notifyUsersAboutEnergyRegen() {
        for (final var personageId : personageDao.getPersonagesWithRecoveredEnergy()) {
            logger.info("Personage {} recovered energy", personageId);
            fullEnergyNotificationCommand.notifyAboutFullEnergy(personageId)
                // Тут сайд эффект, энергия автоматом регенерируется при получении персонажа
                .peek(_ -> personageService.getByIdForce(personageId));
        }
    }
}
