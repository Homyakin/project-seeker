package ru.homyakin.seeker.telegram.command.user.outpost;

import java.util.Optional;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.outpost.action.OutpostService;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.top.TopService;
import ru.homyakin.seeker.locale.outpost.OutpostLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class OutpostDonateTgService {
    private final OutpostService outpostService;
    private final PersonageService personageService;
    private final TopService topService;
    private final GroupTgService groupTgService;
    private final TelegramSender telegramSender;

    public OutpostDonateTgService(
        OutpostService outpostService,
        PersonageService personageService,
        TopService topService,
        GroupTgService groupTgService,
        TelegramSender telegramSender
    ) {
        this.outpostService = outpostService;
        this.personageService = personageService;
        this.topService = topService;
        this.groupTgService = groupTgService;
        this.telegramSender = telegramSender;
    }

    public void donate(User user, Building building, long itemId, Optional<Integer> editMessageId) {
        final var language = user.language();
        final var result = outpostService.tryDonateItemToBuilding(
            user.personageId(),
            building,
            itemId
        );
        result.peekLeft(error -> sendPrivate(
            user,
            OutpostLocalization.donateItemError(language, error),
            editMessageId
        ));
        result.peek(success -> {
            sendPrivate(
                user,
                OutpostLocalization.donateItemSuccessPrivate(language, success),
                editMessageId
            );
            if (!success.completed()) {
                return;
            }
            final var groupIdOpt = personageService.getByIdForce(user.personageId()).memberGroupId();
            if (groupIdOpt.isEmpty()) {
                return;
            }
            final var groupTg = groupTgService.forceGet(groupIdOpt.get());
            final var groupText = OutpostLocalization.groupBuildingCompletedWithTop(
                groupTg.language(),
                building,
                success.newLevel(),
                topService.getTopOutpostBuildingMaterials(success.topContributors())
            );
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(groupTg.id())
                .text(groupText)
                .build()
            );
        });
    }

    private void sendPrivate(User user, String text, Optional<Integer> editMessageId) {
        if (editMessageId.isPresent()) {
            telegramSender.send(
                EditMessageTextBuilder.builder()
                    .chatId(user.id())
                    .messageId(editMessageId.get())
                    .text(text)
                    .build()
            );
            return;
        }
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
            .build()
        );
    }
}
