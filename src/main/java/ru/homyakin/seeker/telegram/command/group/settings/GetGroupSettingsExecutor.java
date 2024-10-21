package ru.homyakin.seeker.telegram.command.group.settings;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.locale.group_settings.GroupSettingsLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GetGroupSettingsExecutor extends CommandExecutor<GetGroupSettings> {
    private final GroupTgService groupTgService;
    private final GetGroup getGroup;
    private final TelegramSender telegramSender;

    public GetGroupSettingsExecutor(
        GroupTgService groupTgService, GetGroup getGroup,
        TelegramSender telegramSender
    ) {
        this.groupTgService = groupTgService;
        this.getGroup = getGroup;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GetGroupSettings command) {
        final var groupTg = groupTgService.getOrCreate(command.groupId());
        final var group = getGroup.forceGet(groupTg.domainGroupId());
        telegramSender.send(
            SendMessageBuilder
                .builder()
                .chatId(command.groupId())
                .text(GroupSettingsLocalization.groupSettings(groupTg.language(), group.settings()))
                .keyboard(InlineKeyboards.eventIntervalsKeyboard(groupTg.language(), group.settings().eventIntervals()))
                .build()
        );
    }
}
