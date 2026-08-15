package ru.homyakin.seeker.telegram.command.user.battle_position;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.battle.BattleLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class UserChangeBattlePositionExecutor extends CommandExecutor<UserChangeBattlePosition> {
    private final UserService userService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public UserChangeBattlePositionExecutor(
        UserService userService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(UserChangeBattlePosition command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var personage = personageService.getByIdForce(user.personageId());
        final var text = BattleLocalization.chooseBattlePosition(user.language());
        final var keyboard = InlineKeyboards.battlePositionKeyboard(user.language(), personage.position());
        if (command.messageId().isPresent()) {
            telegramSender.send(EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(command.messageId().get())
                .text(text)
                .keyboard(keyboard)
                .build()
            );
        } else {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(text)
                .keyboard(keyboard)
                .build()
            );
        }
    }
}
