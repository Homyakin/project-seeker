package ru.homyakin.seeker.telegram.command.group.top;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.top.TopService;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class TopRaidWeekGroupExecutor extends CommandExecutor<TopRaidWeekGroup> {
    private final GroupUserService groupUserService;
    private final TopService topService;
    private final TelegramSender telegramSender;

    public TopRaidWeekGroupExecutor(
        GroupUserService groupUserService,
        TopService topService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.topService = topService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(TopRaidWeekGroup command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId());
        final var group = groupUser.first();
        final var user = groupUser.second();
        final var top = topService.getTopRaidWeekGroup(group.domainGroupId());
        telegramSender.send(
            SendMessageBuilder.builder()
                .text(top.toLocalizedString(group.language(), user.personageId()))
                .chatId(group.id())
                .build()
        );
    }
}
