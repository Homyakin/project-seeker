package ru.homyakin.seeker.telegram.command.group.spin;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.EverydaySpinService;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class SpinTopExecutor extends CommandExecutor<SpinTop> {
    private final EverydaySpinService everydaySpinService;
    private final GroupService groupService;
    private final TelegramSender telegramSender;

    public SpinTopExecutor(
        EverydaySpinService everydaySpinService,
        GroupService groupService,
        TelegramSender telegramSender
    ) {
        this.everydaySpinService = everydaySpinService;
        this.groupService = groupService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(SpinTop command) {
        final var group = groupService.getOrCreate(command.groupId());
        final var count = everydaySpinService.getSpinCountForGroup(command.groupId());
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(command.groupId())
            .text(count.text(group.language()))
            .build()
        );
    }
}
