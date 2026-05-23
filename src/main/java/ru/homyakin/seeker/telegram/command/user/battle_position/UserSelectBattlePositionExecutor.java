package ru.homyakin.seeker.telegram.command.user.battle_position;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.battle.BattleLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class UserSelectBattlePositionExecutor extends CommandExecutor<UserSelectBattlePosition> {
    private final UserService userService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public UserSelectBattlePositionExecutor(
        UserService userService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(UserSelectBattlePosition command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var personage = personageService.setBattlePosition(user.personageId(), command.position());
        telegramSender.send(EditMessageTextBuilder.builder()
            .chatId(user.id())
            .messageId(command.messageId())
            .text(BattleLocalization.chooseBattlePosition(user.language()))
            .keyboard(InlineKeyboards.battlePositionKeyboard(user.language(), personage.position()))
            .build()
        );
    }
}
