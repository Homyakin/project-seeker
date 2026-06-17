package ru.homyakin.seeker.telegram.command.user.badge;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.badge.action.PersonageBadgeService;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.command.common.badge.BadgeUtils;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class ShowPersonageBadgesExecutor extends CommandExecutor<ShowPersonageBadges> {
    private final UserService userService;
    private final PersonageBadgeService personageBadgeService;
    private final TelegramSender telegramSender;

    public ShowPersonageBadgesExecutor(
        UserService userService,
        TelegramSender telegramSender,
        PersonageBadgeService personageBadgeService
    ) {
        this.userService = userService;
        this.personageBadgeService = personageBadgeService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(ShowPersonageBadges command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var badges = personageBadgeService.getAvailableBadges(user.personageId());
        telegramSender.send(BadgeUtils.showBadges(user.id(), user.language(), badges));
    }
}
