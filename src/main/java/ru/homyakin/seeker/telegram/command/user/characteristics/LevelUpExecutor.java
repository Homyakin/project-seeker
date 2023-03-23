package ru.homyakin.seeker.telegram.command.user.characteristics;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.personal.CharacteristicLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

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
        final var personage = personageService.getByIdForce(user.personageId());
        if (personage.characteristics().hasUnspentLevelingPoints()) {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(CharacteristicLocalization.chooseCharacteristic(user.language()))
                .keyboard(InlineKeyboards.chooseCharacteristicsKeyboard(user.language()))
                .build()
            );
        } else {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(CharacteristicLocalization.notEnoughCharacteristicPoints(user.language()))
                .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
                .build()
            );
        }

    }
}
