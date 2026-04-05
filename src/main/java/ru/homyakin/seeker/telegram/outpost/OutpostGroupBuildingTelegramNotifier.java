package ru.homyakin.seeker.telegram.outpost;

import java.util.ArrayList;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.outpost.action.OutpostGroupBuildingCompletedNotifier;
import ru.homyakin.seeker.game.outpost.entity.OutpostBuildingCompletion;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.outpost.OutpostLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class OutpostGroupBuildingTelegramNotifier implements OutpostGroupBuildingCompletedNotifier {
    private final PersonageService personageService;
    private final TelegramSender telegramSender;
    private final GroupTgService groupTgService;

    public OutpostGroupBuildingTelegramNotifier(
        PersonageService personageService,
        TelegramSender telegramSender,
        GroupTgService groupTgService
    ) {
        this.personageService = personageService;
        this.telegramSender = telegramSender;
        this.groupTgService = groupTgService;
    }

    public void notifyGroup(GroupId groupId, Iterable<OutpostBuildingCompletion> completions) {
        final var groupTg = groupTgService.forceGet(groupId);
        final var language = groupTg.language();
        for (final var completion : completions) {
            final var contributorLines = new ArrayList<String>();
            for (final var c : completion.topContributors()) {
                final var name = personageService.getById(c.personageId())
                    .map(LocaleUtils::personageNameWithBadge)
                    .orElseGet(() -> "#" + c.personageId().value());
                contributorLines.add(
                    OutpostLocalization.groupBuildingCompletedContributorLine(language, name, c.materials())
                );
            }
            final var groupText = OutpostLocalization.groupBuildingCompletedWithTop(
                language,
                completion.building(),
                completion.newLevel(),
                contributorLines
            );
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(groupTg.id())
                .text(groupText)
                .build()
            );
        }
    }
}
