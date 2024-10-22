package ru.homyakin.seeker.telegram.command.group.stats;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.game.stats.action.GroupPersonageStatsService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GetPersonageStatsExecutor extends CommandExecutor<GetPersonageStats> {
    private final GroupUserService groupUserService;
    private final GroupPersonageStatsService groupPersonageStatsService;
    private final TelegramSender telegramSender;

    public GetPersonageStatsExecutor(
        GroupUserService groupUserService,
        GroupPersonageStatsService groupPersonageStatsService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.groupPersonageStatsService = groupPersonageStatsService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GetPersonageStats command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId());
        final var groupStats = groupPersonageStatsService.getOrCreate(
            groupUser.first().domainGroupId(),
            groupUser.second().personageId()
        );
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(command.groupId())
            .replyMessageId(command.messageId())
            .text(CommonLocalization.personageGroupStats(groupUser.first().language(), groupStats))
            .build()
        );
    }

}
