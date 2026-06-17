package ru.homyakin.seeker.telegram.command.group.world_raid;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.world_raid.action.GroupWorldRaidBattleResultCommand;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.world_raid.WorldRaidLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GroupWorldRaidReportExecutor extends CommandExecutor<GroupWorldRaidReport> {
    private final GroupTgService groupTgService;
    private final GetGroup getGroup;
    private final GroupWorldRaidBattleResultCommand groupWorldRaidBattleResultCommand;
    private final TelegramSender telegramSender;

    public GroupWorldRaidReportExecutor(
        GroupTgService groupTgService,
        GetGroup getGroup,
        GroupWorldRaidBattleResultCommand groupWorldRaidBattleResultCommand,
        TelegramSender telegramSender
    ) {
        this.groupTgService = groupTgService;
        this.getGroup = getGroup;
        this.groupWorldRaidBattleResultCommand = groupWorldRaidBattleResultCommand;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GroupWorldRaidReport command) {
        final var group = groupTgService.getOrCreate(command.groupTgId());
        final var result = groupWorldRaidBattleResultCommand.getForLastWorldRaid(group.domainGroupId());
        final String text;
        if (result.isEmpty()) {
            text = WorldRaidLocalization.groupWorldRaidReportNotFound(group.language());
        } else {
            text = CommonLocalization.shortGroupBattleReport(
                group.language(),
                result.get(),
                getGroup.forceGet(group.domainGroupId())
            );
        }
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(group.id())
            .text(text)
            .replyMessageId(command.messageId())
            .build()
        );
    }
}
