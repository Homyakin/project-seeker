package ru.homyakin.seeker.telegram.command.group.stats;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.group.stats.GroupStatsService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GetGroupStatsExecutor extends CommandExecutor<GetGroupStats> {
    private final GroupTgService groupTgService;
    private final GroupStatsService groupStatsService;
    private final TelegramSender telegramSender;

    public GetGroupStatsExecutor(
        GroupTgService groupTgService,
        GroupStatsService groupStatsService,
        TelegramSender telegramSender
    ) {
        this.groupTgService = groupTgService;
        this.groupStatsService = groupStatsService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GetGroupStats command) {
        final var group = groupTgService.getOrCreate(command.groupId());
        final var groupStats = groupStatsService.findById(command.groupId())
            .orElseThrow(() -> new IllegalStateException("No stats for group " + command.groupId()));
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(command.groupId())
            .text(CommonLocalization.groupStats(group.language(), groupStats))
            .build()
        );
    }

}
