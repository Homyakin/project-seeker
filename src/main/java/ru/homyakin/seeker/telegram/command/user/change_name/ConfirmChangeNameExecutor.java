package ru.homyakin.seeker.telegram.command.user.change_name;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.personal.ChangeNameLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.state.UserStateService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class ConfirmChangeNameExecutor extends CommandExecutor<ConfirmChangeName> {
    private final UserService userService;
    private final PersonageService personageService;
    private final UserStateService userStateService;
    private final TelegramSender telegramSender;

    public ConfirmChangeNameExecutor(
        UserService userService,
        PersonageService personageService,
        UserStateService userStateService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.userStateService = userStateService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(ConfirmChangeName command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        final var message = personageService.changeName(
            personageService.getByIdForce(user.personageId()),
            command.name()
        ).fold(
            error -> ChangeNameLocalization.internalError(user.language()),
            success -> ChangeNameLocalization.successNameChange(user.language())
        );
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(message)
            .keyboard(ReplyKeyboards.receptionDeskKeyboard(user.language()))
            .build()
        );
        userStateService.clearUserState(user);
    }
}
