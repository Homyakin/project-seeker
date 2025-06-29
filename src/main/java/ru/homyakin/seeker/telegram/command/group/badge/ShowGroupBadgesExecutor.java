package ru.homyakin.seeker.telegram.command.group.badge;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.badge.action.GroupBadgeService;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.command.common.badge.BadgeUtils;
import ru.homyakin.seeker.telegram.group.GroupTgService;

@Component
public class ShowGroupBadgesExecutor extends CommandExecutor<ShowGroupBadges> {
    private final GroupTgService groupTgService;
    private final GroupBadgeService groupBadgeService;
    private final TelegramSender telegramSender;

    public ShowGroupBadgesExecutor(
        GroupTgService groupTgService,
        TelegramSender telegramSender,
        GroupBadgeService groupBadgeService
    ) {
        this.groupTgService = groupTgService;
        this.groupBadgeService = groupBadgeService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(ShowGroupBadges command) {
        final var group = groupTgService.getOrCreate(command.groupTgId());
        final var badges = groupBadgeService.getAvailableBadges(group.domainGroupId());
        telegramSender.send(BadgeUtils.showBadges(group.id(), group.language(), badges));
    }
}
