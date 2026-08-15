package ru.homyakin.seeker.telegram.command.user.targeting_tactic;

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
public class UserChangeTargetingTacticExecutor extends CommandExecutor<UserChangeTargetingTactic> {
    private final UserService userService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public UserChangeTargetingTacticExecutor(
        UserService userService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(UserChangeTargetingTactic command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var personage = personageService.getByIdForce(user.personageId());
        final var text = BattleLocalization.chooseTargetingTactic(
            user.language(),
            personage.targetingTactic()
        );
        final var keyboard = InlineKeyboards.targetingTacticKeyboard(
            user.language(),
            personage.targetingTactic()
        );
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
