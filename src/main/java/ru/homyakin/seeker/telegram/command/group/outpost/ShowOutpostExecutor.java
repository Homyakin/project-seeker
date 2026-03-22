package ru.homyakin.seeker.telegram.command.group.outpost;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.outpost.action.OutpostService;
import ru.homyakin.seeker.locale.outpost.OutpostLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class ShowOutpostExecutor extends CommandExecutor<ShowOutpost> {
    private final GroupTgService groupTgService;
    private final OutpostService outpostService;
    private final TelegramSender telegramSender;

    public ShowOutpostExecutor(
        GroupTgService groupTgService,
        OutpostService outpostService,
        TelegramSender telegramSender
    ) {
        this.groupTgService = groupTgService;
        this.outpostService = outpostService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(ShowOutpost command) {
        final var group = groupTgService.getOrCreate(command.groupId());
        final var slots = outpostService.listSlots(group.domainGroupId());
        final var text = OutpostLocalization.outpost(group.language(), slots);
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(command.groupId())
            .text(text)
            .build()
        );
    }
}
