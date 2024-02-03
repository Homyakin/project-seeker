package ru.homyakin.seeker.telegram.command.user.badge;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.badge.ActivatePersonageBadgeError;
import ru.homyakin.seeker.game.personage.badge.BadgeService;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.personal.BadgeLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class SelectBadgeExecutor extends CommandExecutor<SelectBadge> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final BadgeService badgeService;

    public SelectBadgeExecutor(
        UserService userService,
        TelegramSender telegramSender,
        BadgeService badgeService
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.badgeService = badgeService;
    }

    @Override
    public void execute(SelectBadge command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        final var badge = badgeService.getById(command.badgeId());
        if (badge.isEmpty()) {
            sendNotAvailableError(command, user.language());
            return;
        }
        badgeService.activatePersonageBadge(user.personageId(), badge.get())
            .map(success -> badgeService.getPersonageAvailableBadges(user.personageId()))
            .peek(
                badges -> telegramSender.send(
                    EditMessageTextBuilder.builder()
                        .chatId(user.id())
                        .text(BadgeLocalization.availableBadges(user.language(), badges))
                        .keyboard(InlineKeyboards.badgeSelector(badges))
                        .messageId(command.messageId())
                        .build()
                )
            )
            .peekLeft(
                error -> {
                    final Runnable runnable = switch (error) {
                        case ActivatePersonageBadgeError.BadgeIsNotAvailable ignored ->
                            () -> sendNotAvailableError(command, user.language());
                        case ActivatePersonageBadgeError.AlreadyActivated ignored -> () -> {};
                    };
                    runnable.run();
                }
            );
    }

    private void sendNotAvailableError(SelectBadge command, Language language) {
        telegramSender.send(
            TelegramMethods.createAnswerCallbackQuery(command.callbackId(), BadgeLocalization.badgeIsNotAvailable(language))
        );
    }
}
