package ru.homyakin.seeker.telegram.command.group.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
class JoinGroupExecutor extends CommandExecutor<JoinGroup> {
    private final GroupTgService groupTgService;
    private final TelegramSender telegramSender;

    public JoinGroupExecutor(GroupTgService groupTgService, TelegramSender telegramSender) {
        this.groupTgService = groupTgService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(JoinGroup command) {
        final var group = groupTgService.getOrCreate(command.groupId());
        telegramSender.send(
            SendMessageBuilder.builder().chatId(group.id()).text(CommonLocalization.welcomeGroup(group.language())).build()
        );
    }
}
