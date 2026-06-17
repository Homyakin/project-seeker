package ru.homyakin.seeker.telegram.outpost;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.outpost.action.OutpostGroupBuildingCompletedNotifier;
import ru.homyakin.seeker.game.outpost.entity.OutpostBuildingCompletion;
import ru.homyakin.seeker.game.top.TopService;
import ru.homyakin.seeker.locale.outpost.OutpostLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class OutpostGroupBuildingTelegramNotifier implements OutpostGroupBuildingCompletedNotifier {
    private final TopService topService;
    private final TelegramSender telegramSender;
    private final GroupTgService groupTgService;

    public OutpostGroupBuildingTelegramNotifier(
        TopService topService,
        TelegramSender telegramSender,
        GroupTgService groupTgService
    ) {
        this.topService = topService;
        this.telegramSender = telegramSender;
        this.groupTgService = groupTgService;
    }

    public void notifyGroup(GroupId groupId, Iterable<OutpostBuildingCompletion> completions) {
        final var groupTg = groupTgService.forceGet(groupId);
        final var language = groupTg.language();
        for (final var completion : completions) {
            final var groupText = OutpostLocalization.groupBuildingCompletedWithTop(
                language,
                completion.building(),
                completion.newLevel(),
                topService.getTopOutpostBuildingMaterials(completion.topContributors())
            );
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(groupTg.id())
                .text(groupText)
                .build()
            );
        }
    }
}
