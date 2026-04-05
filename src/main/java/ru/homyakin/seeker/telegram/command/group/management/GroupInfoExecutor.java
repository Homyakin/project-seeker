package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.game.group.action.GroupTaxService;
import ru.homyakin.seeker.game.outpost.action.GroupPassiveEffectsService;
import ru.homyakin.seeker.game.outpost.action.SyncGroupTaxCommand;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GroupInfoExecutor extends CommandExecutor<GroupInfo> {
    private final GroupTgService groupTgService;
    private final GetGroup getGroup;
    private final GroupTaxService groupTaxService;
    private final SyncGroupTaxCommand syncGroupTaxCommand;
    private final GroupPassiveEffectsService groupPassiveEffectsService;
    private final TelegramSender telegramSender;

    public GroupInfoExecutor(
        GroupTgService groupTgService,
        GetGroup getGroup,
        GroupTaxService groupTaxService,
        SyncGroupTaxCommand syncGroupTaxCommand,
        GroupPassiveEffectsService groupPassiveEffectsService,
        TelegramSender telegramSender
    ) {
        this.groupTgService = groupTgService;
        this.getGroup = getGroup;
        this.groupTaxService = groupTaxService;
        this.syncGroupTaxCommand = syncGroupTaxCommand;
        this.groupPassiveEffectsService = groupPassiveEffectsService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GroupInfo command) {
        final var groupTg = groupTgService.getOrCreate(command.groupTgId());
        final var groupId = groupTg.domainGroupId();
        syncGroupTaxCommand.execute(groupId);
        final var group = getGroup.forceGetProfile(groupId);
        final var tax = groupTaxService.groupTaxSnapshot(groupId);
        final var passiveEffects = groupPassiveEffectsService.listPassiveEffects(groupId);
        telegramSender.send(
            SendMessageBuilder
                .builder()
                .chatId(command.groupTgId())
                .text(GroupManagementLocalization.groupInfo(groupTg.language(), group, tax, passiveEffects))
                .build()
        );
    }
}
