package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GroupInfoExecutor extends CommandExecutor<GroupInfo> {
    private final GroupTgService groupTgService;
    private final GetGroup getGroup;
    private final TelegramSender telegramSender;

    public GroupInfoExecutor(
        GroupTgService groupTgService,
        GetGroup getGroup,
        TelegramSender telegramSender
    ) {
        this.groupTgService = groupTgService;
        this.getGroup = getGroup;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GroupInfo command) {
        final var groupTg = groupTgService.getOrCreate(command.groupTgId());
        final var group = getGroup.forceGetProfile(groupTg.domainGroupId());
        telegramSender.send(
            SendMessageBuilder
                .builder()
                .chatId(command.groupTgId())
                .text(GroupManagementLocalization.groupInfo(groupTg.language(), group))
                .build()
        );
    }
}
