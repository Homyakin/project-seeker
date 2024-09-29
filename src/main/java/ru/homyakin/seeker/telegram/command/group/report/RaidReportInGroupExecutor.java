package ru.homyakin.seeker.telegram.command.group.report;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.service.GroupEventService;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.raid.RaidLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class RaidReportInGroupExecutor extends CommandExecutor<RaidReportInGroup> {
    private final GroupUserService groupUserService;
    private final PersonageService personageService;
    private final GroupEventService groupEventService;
    private final ItemService itemService;
    private final TelegramSender telegramSender;

    public RaidReportInGroupExecutor(
        GroupUserService groupUserService,
        PersonageService personageService,
        GroupEventService groupEventService,
        ItemService itemService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.personageService = personageService;
        this.groupEventService = groupEventService;
        this.itemService = itemService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(RaidReportInGroup command) {
        final var groupUserPair = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId());
        final var group = groupUserPair.first();
        final var user = groupUserPair.second();
        final var text = groupEventService.getLastEndedEventInGroup(group.id())
            .flatMap(groupEvent -> personageService.getRaidResult(user.personageId(), groupEvent.launchedEventId()))
            .map(result -> {
                final var personage = personageService.getByIdForce(user.personageId());
                return RaidLocalization.shortPersonageReport(
                    group.language(),
                    result,
                    personage,
                    result.generatedItemId().flatMap(itemService::getById)
                );
            })
            .orElseGet(() -> RaidLocalization.lastGroupRaidReportNotFound(group.language()));
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(group.id())
            .text(text)
            .replyMessageId(command.messageId())
            .build()
        );
    }

}
