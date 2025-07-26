package ru.homyakin.seeker.telegram.command.group.top;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.top.TopService;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class TopGroupRaidLevelExecutor extends CommandExecutor<TopGroupRaidLevel> {
    private final GroupTgService groupTgService;
    private final TopService topService;
    private final TelegramSender telegramSender;

    public TopGroupRaidLevelExecutor(
        GroupTgService groupTgService,
        TopService topService,
        TelegramSender telegramSender
    ) {
        this.groupTgService = groupTgService;
        this.topService = topService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(TopGroupRaidLevel command) {
        final var group = groupTgService.getOrCreate(command.groupId());
        final var top = topService.getGroupTopRaidLevel();
        telegramSender.send(
            SendMessageBuilder.builder()
                .text(top.toLocalizedString(group.language(), group.domainGroupId()))
                .chatId(group.id())
                .build()
        );
    }
}
