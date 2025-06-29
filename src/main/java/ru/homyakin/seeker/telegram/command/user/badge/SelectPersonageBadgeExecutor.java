package ru.homyakin.seeker.telegram.command.user.badge;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.badge.action.PersonageBadgeService;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.command.common.badge.BadgeUtils;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class SelectPersonageBadgeExecutor extends CommandExecutor<SelectPersonageBadge> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final PersonageBadgeService personageBadgeService;

    public SelectPersonageBadgeExecutor(
        UserService userService,
        TelegramSender telegramSender,
        PersonageBadgeService personageBadgeService
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.personageBadgeService = personageBadgeService;
    }

    @Override
    public void execute(SelectPersonageBadge command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        personageBadgeService.activateBadge(user.personageId(), command.badgeId())
            .map(_ -> personageBadgeService.getAvailableBadges(user.personageId()))
            .peek(
                badges -> telegramSender.send(
                    BadgeUtils.editBadges(user.id(), command.messageId(), user.language(), badges)
                )
            )
            .peekLeft(
                error -> BadgeUtils.processSelectError(error, command.callbackId(), user.language())
                    .ifPresent(telegramSender::send)
            );
    }
}
