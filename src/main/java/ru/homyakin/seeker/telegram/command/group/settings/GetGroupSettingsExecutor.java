package ru.homyakin.seeker.telegram.command.group.settings;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.group_settings.GroupSettingsLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GetGroupSettingsExecutor extends CommandExecutor<GetGroupSettings> {
    private final GroupService groupService;
    private final TelegramSender telegramSender;

    public GetGroupSettingsExecutor(
        GroupService groupService,
        TelegramSender telegramSender
    ) {
        this.groupService = groupService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GetGroupSettings command) {
        final var group = groupService.getOrCreate(command.groupId());

        telegramSender.send(
            SendMessageBuilder
                .builder()
                .chatId(command.groupId())
                .text(GroupSettingsLocalization.groupSettings(group.language(), group.settings()))
                .keyboard(InlineKeyboards.eventIntervalsKeyboard(group.language(), group.settings().eventIntervals()))
                .build()
        );
    }
}
