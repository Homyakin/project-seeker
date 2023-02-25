package ru.homyakin.seeker.telegram.command.user.level;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.personal.LevelingLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class LevelUpExecutor extends CommandExecutor<LevelUp> {
    private final UserService userService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public LevelUpExecutor(
        UserService userService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(LevelUp command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        final var personage = personageService.getById(user.personageId())
                .orElseThrow(() -> new IllegalStateException("Personage must be present at user"));
        if (personage.characteristics().hasUnspentLevelingPoints()) {
            telegramSender.send(
                TelegramMethods.createSendMessage(
                    user.id(),
                    LevelingLocalization.chooseLevelUpCharacteristic(user.language()),
                    ReplyKeyboards.levelUpKeyboard()
                )
            );
        } else {
            telegramSender.send(
                TelegramMethods.createSendMessage(
                    user.id(),
                    LevelingLocalization.notEnoughLevelingPoints(user.language()),
                    ReplyKeyboards.mainKeyboard(user.language())
                )
            );
        }

    }
}
