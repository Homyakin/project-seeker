package ru.homyakin.seeker.telegram.command.user.characteristics;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.CharacteristicType;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.personal.CharacteristicLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;

@Component
public class IncreaseCharacteristicExecutor extends CommandExecutor<IncreaseCharacteristic> {
    private final UserService userService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public IncreaseCharacteristicExecutor(
        UserService userService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(IncreaseCharacteristic command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());

        final var personage = personageService.getByIdForce(user.personageId());
        (
            switch (command.characteristicType()) {
                case STRENGTH -> personageService.incrementStrength(personage);
                case AGILITY -> personageService.incrementAgility(personage);
                case WISDOM -> personageService.incrementWisdom(personage);
            }
        ).peek(it ->
            telegramSender.send(EditMessageTextBuilder.builder()
                .chatId(command.userId())
                .messageId(command.messageId())
                .text(successText(user.language(), it.characteristics(), command.characteristicType()))
                .keyboard(keyboardByCharacteristics(user.language(), it.characteristics()))
                .build()
            )
        ).peekLeft(error ->
            telegramSender.send(EditMessageTextBuilder.builder()
                .chatId(command.userId())
                .messageId(command.messageId())
                .text(CharacteristicLocalization.notEnoughCharacteristicPoints(user.language()))
                .build()
            )
        );
    }

    private String successText(
        Language language,
        Characteristics characteristics,
        CharacteristicType increasedType
    ) {
        var string = CharacteristicLocalization.increasedCharacteristic(language, increasedType, 1)
            + "\n"
            + CharacteristicLocalization.currentCharacteristics(language, characteristics);
        if (characteristics.hasUnspentLevelingPoints()) {
            string += "\n\n" + CharacteristicLocalization.chooseCharacteristic(language);
        }
        return string;
    }

    private InlineKeyboardMarkup keyboardByCharacteristics(
        Language language,
        Characteristics characteristics
    ) {
        return characteristics.hasUnspentLevelingPoints()
            ? InlineKeyboards.chooseCharacteristicsKeyboard(language)
            : null;
    }
}
