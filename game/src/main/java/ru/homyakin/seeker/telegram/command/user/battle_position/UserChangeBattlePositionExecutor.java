package ru.homyakin.seeker.telegram.command.user.battle_position;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.battle.BattleLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
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
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(BattleLocalization.chooseBattlePosition(user.language()))
            .keyboard(InlineKeyboards.battlePositionKeyboard(user.language(), personage.position()))
            .build()
        );
    }
}
