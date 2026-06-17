package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.GroupTaxService;
import ru.homyakin.seeker.game.outpost.action.SyncGroupTaxCommand;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GroupTaxExecutor extends CommandExecutor<GroupTax> {
    private final GroupTgService groupTgService;
    private final GroupTaxService groupTaxService;
    private final SyncGroupTaxCommand syncGroupTaxCommand;
    private final TelegramSender telegramSender;

    public GroupTaxExecutor(
        GroupTgService groupTgService,
        GroupTaxService groupTaxService,
        SyncGroupTaxCommand syncGroupTaxCommand,
        TelegramSender telegramSender
    ) {
        this.groupTgService = groupTgService;
        this.groupTaxService = groupTaxService;
        this.syncGroupTaxCommand = syncGroupTaxCommand;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GroupTax command) {
        final var groupTg = groupTgService.getOrCreate(command.groupTgId());
        final var groupId = groupTg.domainGroupId();
        syncGroupTaxCommand.execute(groupId);
        final var tax = groupTaxService.groupTaxSnapshot(groupId);
        telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(command.groupTgId())
                .text(GroupManagementLocalization.groupTaxDetails(groupTg.language(), tax))
                .build()
        );
    }
}
