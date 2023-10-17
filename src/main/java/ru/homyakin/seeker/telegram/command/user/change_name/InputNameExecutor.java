package ru.homyakin.seeker.telegram.command.user.change_name;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.personal.ChangeNameLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.state.ChangeNameState;
import ru.homyakin.seeker.telegram.user.state.UserStateService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class InputNameExecutor extends CommandExecutor<InputName> {
    private final UserService userService;
    private final PersonageService personageService;
    private final UserStateService userStateService;
    private final TelegramSender telegramSender;

    public InputNameExecutor(
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
    public void execute(InputName command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        if (command.name().isBlank()) {
            telegramSender.send(
                SendMessageBuilder.builder().chatId(user.id()).text(ChangeNameLocalization.changeNameWithoutName(user.language())).build()
            );
            return;
        }
        final var messageBuilder = SendMessageBuilder.builder().chatId(user.id());
        personageService.getByIdForce(user.personageId())
            .validateName(command.name())
            .fold(
                error -> messageBuilder.text(error.toUserMessage(user.language())),
                name -> {
                    userStateService.setUserState(user, new ChangeNameState.ConfirmChangeName(name));
                    messageBuilder.text(ChangeNameLocalization.confirmName(user.language(), name));
                    messageBuilder.keyboard(ReplyKeyboards.confirmChangeNameKeyboard(user.language()));
                    return null;
                }
            );

        telegramSender.send(messageBuilder.build());
    }
}
