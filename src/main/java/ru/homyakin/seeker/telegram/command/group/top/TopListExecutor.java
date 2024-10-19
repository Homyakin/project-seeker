package ru.homyakin.seeker.telegram.command.group.top;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.top.TopLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class TopListExecutor extends CommandExecutor<TopList> {
    private final GroupTgService groupTgService;
    private final TelegramSender telegramSender;

    public TopListExecutor(
        GroupTgService groupTgService,
        TelegramSender telegramSender
    ) {
        this.groupTgService = groupTgService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(TopList command) {
        final var group = groupTgService.getOrCreate(command.groupId());
        telegramSender.send(
            SendMessageBuilder.builder()
                .text(TopLocalization.topList(group.language()))
                .chatId(group.id())
                .build()
        );
    }
}
