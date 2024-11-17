package ru.homyakin.seeker.telegram.command.user.badge;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.badge.BadgeService;
import ru.homyakin.seeker.locale.personal.BadgeLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class ShowBadgesExecutor extends CommandExecutor<ShowBadges> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final BadgeService badgeService;

    public ShowBadgesExecutor(
        UserService userService,
        TelegramSender telegramSender,
        BadgeService badgeService
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.badgeService = badgeService;
    }

    @Override
    public void execute(ShowBadges command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var badges = badgeService.getPersonageAvailableBadges(user.personageId());
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(BadgeLocalization.availableBadges(user.language(), badges))
            .keyboard(InlineKeyboards.badgeSelector(badges))
            .build()
        );
    }
}
