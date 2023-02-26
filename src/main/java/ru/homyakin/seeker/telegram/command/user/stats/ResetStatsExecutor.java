package ru.homyakin.seeker.telegram.command.user.stats;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.personal.LevelingLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.command.user.navigation.StartUser;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class ResetStatsExecutor extends CommandExecutor<ResetStats> {
    private final UserService userService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public ResetStatsExecutor(
        UserService userService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(ResetStats command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        final var personage = personageService.getByIdForce(user.personageId());
        personageService.resetStats(personage)
            .peek(it ->
                telegramSender.send(
                    TelegramMethods.createSendMessage(
                        user.id(),
                        LevelingLocalization.successResetStats(user.language()),
                        ReplyKeyboards.levelUpKeyboard()
                    )
                )
            ).peekLeft(it ->
                telegramSender.send(
                    TelegramMethods.createSendMessage(
                        user.id(),
                        LevelingLocalization.notEnoughMoney(user.language(), it.neededMoney())
                    )
                )
            );
    }
}
