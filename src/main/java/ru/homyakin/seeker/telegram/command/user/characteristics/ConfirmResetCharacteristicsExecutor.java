package ru.homyakin.seeker.telegram.command.user.characteristics;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.personal.CharacteristicLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class ConfirmResetCharacteristicsExecutor extends CommandExecutor<ConfirmResetCharacteristics> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final PersonageService personageService;

    public ConfirmResetCharacteristicsExecutor(
        UserService userService,
        TelegramSender telegramSender,
        PersonageService personageService
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.personageService = personageService;
    }

    @Override
    public void execute(ConfirmResetCharacteristics command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        personageService.resetStats(
                personageService.getByIdForce(user.personageId())
            )
            .peek(
                personage -> telegramSender.send(
                    TelegramMethods.createEditMessageText(
                        command.userId(),
                        command.messageId(),
                        successResetText(user.language(), personage.characteristics()),
                        InlineKeyboards.chooseCharacteristicsKeyboard(user.language())
                    )
                )
            )
            .peekLeft(
                error -> telegramSender.send(
                    TelegramMethods.createEditMessageText(
                        command.userId(),
                        command.messageId(),
                        CharacteristicLocalization.notEnoughMoney(user.language(), error.neededMoney())
                    )
                )
            )
        ;
    }

    private String successResetText(Language language, Characteristics characteristics) {
        return CharacteristicLocalization.successReset(language)
            + "\n"
            + CharacteristicLocalization.currentCharacteristics(language, characteristics);

    }
}

