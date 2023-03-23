package ru.homyakin.seeker.telegram.command.user.profile;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.errors.NameError;
import ru.homyakin.seeker.locale.personal.ChangeNameLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

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
        if (command.name().isBlank()) {
            telegramSender.send(
                SendMessageBuilder.builder().chatId(user.id()).text(ChangeNameLocalization.changeNameWithoutName(user.language())).build()
            );
            return;
        }
        final var personage = personageService.getById(user.personageId()).orElseThrow();
        final var result = personageService.changeName(personage, command.name());
        if (result.isRight()) {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(ChangeNameLocalization.successNameChange(user.language()))
                .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
                .build()
            );
        } else {
            final var error = result.getLeft();
            //TODO switch
            final String message;
            if (error instanceof NameError.InvalidLength invalidLength) {
                message = ChangeNameLocalization.personageNameInvalidLength(
                    user.language(), invalidLength.minLength(), invalidLength.maxLength()
                );
            } else if (error instanceof NameError.NotAllowedSymbols) {
                message = ChangeNameLocalization.personageNameInvalidSymbols(user.language());
            } else {
                throw new IllegalStateException("Unknown error: " + error.toString());
            }
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(message)
                .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
                .build()
            );
        }
    }
}
