package ru.homyakin.seeker.telegram.command.group.top;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.top.TopService;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class TopTavernSpentExecutor extends CommandExecutor<TopTavernSpent> {
    private final TopService topService;
    private final GroupUserService groupUserService;
    private final TelegramSender telegramSender;

    public TopTavernSpentExecutor(
        TopService topService,
        GroupUserService groupUserService,
        TelegramSender telegramSender
    ) {
        this.topService = topService;
        this.groupUserService = groupUserService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(TopTavernSpent command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId());
        final var group = groupUser.first();
        final var user = groupUser.second();
        final var top = topService.getTopTavernSpentGroup(group.domainGroupId());
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(command.groupTgId())
            .text(top.toLocalizedString(group.language(), user.personageId()))
            .build()
        );
    }
}
