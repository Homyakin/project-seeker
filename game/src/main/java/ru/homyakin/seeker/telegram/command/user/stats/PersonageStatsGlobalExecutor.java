package ru.homyakin.seeker.telegram.command.user.stats;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.season.action.SeasonService;
import ru.homyakin.seeker.game.stats.action.PersonageStatsService;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class PersonageStatsGlobalExecutor extends CommandExecutor<PersonageStatsGlobal> {
    private final UserService userService;
    private final PersonageStatsService personageStatsService;
    private final TelegramSender telegramSender;
    private final SeasonService seasonService;

    public PersonageStatsGlobalExecutor(
        UserService userService,
        PersonageStatsService personageStatsService,
        TelegramSender telegramSender,
        SeasonService seasonService
    ) {
        this.userService = userService;
        this.personageStatsService = personageStatsService;
        this.telegramSender = telegramSender;
        this.seasonService = seasonService;
    }

    @Override
    public void execute(PersonageStatsGlobal command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var text = personageStatsService.getForCurrentSeason(user.personageId())
            .map(it -> CommonLocalization.personageGlobalStats(user.language(), it))
            .orElseGet(() -> CommonLocalization.noStatsForSeason(user.language()));
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
            .build()
        );
    }

}
