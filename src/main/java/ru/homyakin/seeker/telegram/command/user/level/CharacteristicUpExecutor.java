package ru.homyakin.seeker.telegram.command.user.level;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.locale.personal.LevelingLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class CharacteristicUpExecutor extends CommandExecutor<CharacteristicUp> {
    private final UserService userService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public CharacteristicUpExecutor(
        UserService userService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(CharacteristicUp command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        final var personage = personageService.getById(user.personageId())
            .orElseThrow(() -> new IllegalStateException("Personage must be present at user"));
        (
            switch (command.characteristicType()) {
                case STRENGTH -> personageService.incrementStrength(personage);
                case AGILITY -> personageService.incrementAgility(personage);
                case WISDOM -> personageService.incrementWisdom(personage);
            }
        ).peek(it -> telegramSender.send(successSendMessage(user, it))
        ).peekLeft(error ->
            telegramSender.send(
                TelegramMethods.createSendMessage(
                    user.id(),
                    LevelingLocalization.notEnoughLevelingPoints(user.language()),
                    ReplyKeyboards.mainKeyboard(user.language())
                )
            )
        );
    }

    private SendMessage successSendMessage(User user, Personage personage) {
        return TelegramMethods.createSendMessage(
            user.id(),
            LevelingLocalization.successLevelUp(user.language()),
            personage.characteristics().hasUnspentLevelingPoints()
                ? null
                : ReplyKeyboards.mainKeyboard(user.language())
        );
    }
}
