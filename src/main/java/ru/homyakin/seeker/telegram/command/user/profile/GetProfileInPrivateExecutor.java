package ru.homyakin.seeker.telegram.command.user.profile;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GetProfileInPrivateExecutor extends CommandExecutor<GetProfileInPrivate> {
    private final UserService userService;
    private final PersonageService personageService;
    private final LaunchedEventService launchedEventService;
    private final TelegramSender telegramSender;

    public GetProfileInPrivateExecutor(
        UserService userService,
        PersonageService personageService,
        LaunchedEventService launchedEventService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.launchedEventService = launchedEventService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GetProfileInPrivate command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var personage = personageService.getByIdForce(user.personageId());
        final var events = launchedEventService.getActiveEventsByPersonageId(personage.id());
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(personage.fullProfile(user.language(), events))
            .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
            .build()
        );
    }

}
