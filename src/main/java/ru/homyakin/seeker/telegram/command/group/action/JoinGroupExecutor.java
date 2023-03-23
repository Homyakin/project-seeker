package ru.homyakin.seeker.telegram.command.group.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
class JoinGroupExecutor extends CommandExecutor<JoinGroup> {
    private final GroupService groupService;
    private final TelegramSender telegramSender;

    public JoinGroupExecutor(GroupService groupService, TelegramSender telegramSender) {
        this.groupService = groupService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(JoinGroup command) {
        final var group = groupService.getOrCreate(command.groupId());
        telegramSender.send(
            SendMessageBuilder.builder().chatId(group.id()).text(CommonLocalization.welcomeGroup(group.language())).build()
        );
    }
}
