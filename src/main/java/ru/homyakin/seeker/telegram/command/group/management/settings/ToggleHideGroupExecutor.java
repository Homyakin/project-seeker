package ru.homyakin.seeker.telegram.command.group.management.settings;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.ToggleGroupIsHiddenCommand;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group_settings.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class ToggleHideGroupExecutor extends CommandExecutor<ToggleHideGroup> {
    private final GroupTgService groupTgService;
    private final GroupUserService groupUserService;
    private final ToggleGroupIsHiddenCommand toggleGroupIsHiddenCommand;
    private final TelegramSender telegramSender;

    public ToggleHideGroupExecutor(
        GroupTgService groupTgService,
        GroupUserService groupUserService,
        ToggleGroupIsHiddenCommand toggleGroupIsHiddenCommand,
        TelegramSender telegramSender
    ) {
        this.groupTgService = groupTgService;
        this.groupUserService = groupUserService;
        this.toggleGroupIsHiddenCommand = toggleGroupIsHiddenCommand;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(ToggleHideGroup command) {
        final var groupTg = groupTgService.getOrCreate(command.groupTgId());
        final var isUserAdmin = groupUserService.isUserAdminInGroup(command.groupTgId(), command.userId());
        if (!isUserAdmin) {
            telegramSender.send(
                SendMessageBuilder
                    .builder()
                    .chatId(command.groupTgId())
                    .text(CommonLocalization.onlyAdminAction(groupTg.language()))
                    .build()
            );
            return;
        }
        final var text = toggleGroupIsHiddenCommand.execute(groupTg.domainGroupId())
            .fold(
                _ -> GroupManagementLocalization.forbiddenHidden(groupTg.language()),
                result -> result
                    ? GroupManagementLocalization.groupIsHidden(groupTg.language())
                    : GroupManagementLocalization.groupIsNotHidden(groupTg.language())
            );
        telegramSender.send(
            SendMessageBuilder
                .builder()
                .chatId(command.groupTgId())
                .text(text)
                .build()
        );
    }
}
