package ru.homyakin.seeker.telegram.command.group.settings;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.group_settings.ActiveTimeLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GetActiveTimeExecutor extends CommandExecutor<GetActiveTime> {
    private final GroupService groupService;
    private final TelegramSender telegramSender;

    public GetActiveTimeExecutor(GroupService groupService, TelegramSender telegramSender) {
        this.groupService = groupService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GetActiveTime command) {
        final var group = groupService.getOrCreate(command.groupId());

        telegramSender.send(
            SendMessageBuilder
                .builder()
                .text(ActiveTimeLocalization.current(group.language(), group.activeTime()))
                .chatId(group.id())
                .build()
        );
    }
}
