package ru.homyakin.seeker.telegram.command.user.stats;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.stats.action.GetPersonageStatsCommand;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class PersonageStatsGlobalExecutor extends CommandExecutor<PersonageStatsGlobal> {
    private final UserService userService;
    private final GetPersonageStatsCommand getPersonageStatsCommand;
    private final TelegramSender telegramSender;

    public PersonageStatsGlobalExecutor(
        UserService userService,
        GetPersonageStatsCommand getPersonageStatsCommand,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.getPersonageStatsCommand = getPersonageStatsCommand;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(PersonageStatsGlobal command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var stats = getPersonageStatsCommand.execute(user.personageId());
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(CommonLocalization.personageGlobalStats(user.language(), stats))
            .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
            .build()
        );
    }

}
