package ru.homyakin.seeker.telegram.command.group.top;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.top.TopLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class TopListExecutor extends CommandExecutor<TopList> {
    private final GroupService groupService;
    private final TelegramSender telegramSender;

    public TopListExecutor(
        GroupService groupService,
        TelegramSender telegramSender
    ) {
        this.groupService = groupService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(TopList command) {
        final var group = groupService.getOrCreate(command.groupId());
        telegramSender.send(
            SendMessageBuilder.builder()
                .text(TopLocalization.topList(group.language()))
                .chatId(group.id())
                .build()
        );
    }
}
