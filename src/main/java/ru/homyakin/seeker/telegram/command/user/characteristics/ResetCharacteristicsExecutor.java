package ru.homyakin.seeker.telegram.command.user.characteristics;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.locale.personal.CharacteristicLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class ResetCharacteristicsExecutor extends CommandExecutor<ResetCharacteristics> {
    private final UserService userService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public ResetCharacteristicsExecutor(
        UserService userService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(ResetCharacteristics command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        final var personage = personageService.getByIdForce(user.personageId());
        if (personage.money().lessThan(Personage.RESET_STATS_COST)) { // TODO вынести проверку в сервис
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(CharacteristicLocalization.notEnoughMoney(user.language(), Personage.RESET_STATS_COST))
                .build()
            );
        } else {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(CharacteristicLocalization.resetConfirmation(user.language(), Personage.RESET_STATS_COST))
                .keyboard(InlineKeyboards.resetCharacteristicsConfirmationKeyboard(user.language()))
                .build()
            );
        }
    }
}
