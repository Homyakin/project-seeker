package ru.homyakin.seeker.telegram.command.user.profile;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.command.CommandType;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class ChangeNameExecutor extends CommandExecutor<ChangeName> {
    private final UserService userService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public ChangeNameExecutor(
        UserService userService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(ChangeName command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        final var newName = command.data().replaceAll(CommandType.CHANGE_NAME.getText(), "").trim();
        if (newName.isBlank()) {
            telegramSender.send(
                TelegramMethods.createSendMessage(
                    command.userId(), Localization.get(user.language()).changeNameWithoutName()
                )
            );
            return;
        }
        final var personage = personageService.getById(user.personageId()).orElseThrow();
        final var result = personageService.changeName(personage, newName);
        if (result.isRight()) {
            telegramSender.send(
                TelegramMethods.createSendMessage(
                    command.userId(),
                    Localization.get(user.language()).successNameChange())
            );
        } else {
            telegramSender.send(
                TelegramMethods.createSendMessage(
                    command.userId(),
                    Localization.get(user.language()).nameTooLong().formatted(Personage.MAX_NAME_LENGTH))
            );
        }
    }
}
