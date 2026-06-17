package ru.homyakin.seeker.telegram.command.group.badge;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.badge.action.GroupBadgeService;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.command.common.badge.BadgeUtils;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class SelectGroupBadgeExecutor extends CommandExecutor<SelectGroupBadge> {
    private final GroupUserService groupUserService;
    private final GroupBadgeService groupBadgeService;
    private final TelegramSender telegramSender;

    public SelectGroupBadgeExecutor(
        TelegramSender telegramSender,
        GroupUserService groupUserService,
        GroupBadgeService groupBadgeService
    ) {
        this.telegramSender = telegramSender;
        this.groupUserService = groupUserService;
        this.groupBadgeService = groupBadgeService;
    }

    @Override
    public void execute(SelectGroupBadge command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId());
        final var group = groupUser.first();
        if (!groupUserService.isUserAdminInGroup(group.id(), command.userId())) {
            telegramSender.send(
                TelegramMethods.createAnswerCallbackQuery(
                    command.callbackId(),
                    CommonLocalization.onlyAdminAction(group.language())
                )
            );
            return;
        }
        groupBadgeService.activateBadge(group.domainGroupId(), command.badgeId())
            .map(_ -> groupBadgeService.getAvailableBadges(group.domainGroupId()))
            .peek(
                badges -> telegramSender.send(
                    BadgeUtils.editBadges(group.id(), command.messageId(), group.language(), badges)
                )
            )
            .peekLeft(
                error -> BadgeUtils.processSelectError(error, command.callbackId(), group.language())
                    .ifPresent(telegramSender::send)
            );
    }
}
