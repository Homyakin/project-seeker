package ru.homyakin.seeker.telegram.command.group.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
class MigrateFromGroupExecutor extends CommandExecutor<MigrateFromGroup> {
    private final GroupTgService groupTgService;
    private final TelegramSender telegramSender;

    public MigrateFromGroupExecutor(GroupTgService groupTgService, TelegramSender telegramSender) {
        this.groupTgService = groupTgService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(MigrateFromGroup command) {
        groupTgService.migrateGroupData(command.from(), command.to());
        final var updatedGroup = groupTgService.getOrCreate(command.to());
        telegramSender.send(
            SendMessageBuilder
                .builder()
                .text(CommonLocalization.migrateGroup(updatedGroup.language()))
                .chatId(updatedGroup.id())
                .build()
        );
    }
}
