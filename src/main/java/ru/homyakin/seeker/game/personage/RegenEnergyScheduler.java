package ru.homyakin.seeker.game.personage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.personal_quest.PersonalQuestService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.notification.action.SendNotificationToPersonageCommand;
import ru.homyakin.seeker.game.personage.notification.entity.Notification;
import ru.homyakin.seeker.game.personage.settings.action.GetPersonageSettingsCommand;

@Component
public class RegenEnergyScheduler {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final PersonageService personageService;
    private final PersonageDao personageDao;
    private final GetPersonageSettingsCommand getPersonageSettingsCommand;
    private final PersonalQuestService personalQuestService;
    private final SendNotificationToPersonageCommand sendNotificationToPersonageCommand;

    public RegenEnergyScheduler(
        PersonageService personageService,
        PersonageDao personageDao,
        GetPersonageSettingsCommand getPersonageSettingsCommand,
        PersonalQuestService personalQuestService,
        SendNotificationToPersonageCommand sendNotificationToPersonageCommand
    ) {
        this.personageService = personageService;
        this.personageDao = personageDao;
        this.getPersonageSettingsCommand = getPersonageSettingsCommand;
        this.personalQuestService = personalQuestService;
        this.sendNotificationToPersonageCommand = sendNotificationToPersonageCommand;
    }

    @Scheduled(cron = "0 * * * * *")
    public void notifyUsersAboutEnergyRegen() {
        for (final var personageId : personageDao.getPersonagesWithRecoveredEnergy()) {
            logger.info("Personage {} recovered energy", personageId);
            final var settings = getPersonageSettingsCommand.execute(personageId);
            if (settings.autoQuesting()) {
                autoStartQuest(personageId);
            } else {
                regenEnergy(personageId);
            }
        }
    }

    private void autoStartQuest(PersonageId personageId) {
        final var result = personalQuestService.autoStartQuest(personageId);
        if (result.isRight()) {
            logger.info("Personage {} auto started personal quest", personageId);
            sendNotificationToPersonageCommand
                .sendNotification(personageId, new Notification.AutoStartQuest(result.get()));
        } else {
            logger.error("Failed auto start quest for personage {}, reason: {}", personageId, result.getLeft());
        }
    }

    private void regenEnergy(PersonageId personageId) {
        sendNotificationToPersonageCommand.sendNotification(personageId, Notification.RecoveredEnergy.INSTANCE)
            // Тут сайд эффект, энергия автоматом регенерируется при получении персонажа
            .peek(_ -> personageService.getByIdForce(personageId));
    }
}
