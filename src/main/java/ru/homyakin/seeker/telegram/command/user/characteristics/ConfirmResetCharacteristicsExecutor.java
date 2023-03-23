package ru.homyakin.seeker.telegram.command.user.characteristics;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.personal.CharacteristicLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;

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
                personage -> telegramSender.send(EditMessageTextBuilder.builder()
                    .chatId(command.userId())
                    .messageId(command.messageId())
                    .text(successResetText(user.language(), personage.characteristics()))
                    .keyboard(InlineKeyboards.chooseCharacteristicsKeyboard(user.language()))
                    .build()
                )
            )
            .peekLeft(
                error -> telegramSender.send(EditMessageTextBuilder.builder()
                    .chatId(command.userId())
                    .messageId(command.messageId())
                    .text(CharacteristicLocalization.notEnoughMoney(user.language(), error.neededMoney()))
                    .build()
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

