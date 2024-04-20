package ru.homyakin.seeker.telegram.command.group.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
class MigrateFromGroupExecutor extends CommandExecutor<MigrateFromGroup> {
    private final GroupService groupService;
    private final TelegramSender telegramSender;

    public MigrateFromGroupExecutor(GroupService groupService, TelegramSender telegramSender) {
        this.groupService = groupService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(MigrateFromGroup command) {
        groupService.getOrCreate(command.to());
        groupService.setNotActive(command.from());
        groupService.migrateGroupDate(command.from(), command.to());
        final var updatedGroup = groupService.getOrCreate(command.to());
        telegramSender.send(
            SendMessageBuilder
                .builder()
                .text(CommonLocalization.migrateGroup(updatedGroup.language()))
                .chatId(updatedGroup.id())
                .build()
        );
    }
}
