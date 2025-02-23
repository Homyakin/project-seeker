package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GroupCommandsExecutor extends CommandExecutor<GroupCommands> {
    private final GroupTgService groupTgService;
    private final TelegramSender telegramSender;

    public GroupCommandsExecutor(
        GroupTgService groupTgService,
        TelegramSender telegramSender
    ) {
        this.groupTgService = groupTgService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GroupCommands command) {
        final var groupTg = groupTgService.getOrCreate(command.groupTgId());

        telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(command.groupTgId())
                .text(GroupManagementLocalization.groupCommands(groupTg.language()))
                .build()
        );
    }
}
