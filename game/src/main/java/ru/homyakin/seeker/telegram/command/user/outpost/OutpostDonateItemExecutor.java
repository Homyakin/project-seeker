package ru.homyakin.seeker.telegram.command.user.outpost;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.outpost.action.OutpostService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.top.TopService;
import ru.homyakin.seeker.locale.outpost.OutpostLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class OutpostDonateItemExecutor extends CommandExecutor<OutpostDonateItem> {
    private final UserService userService;
    private final OutpostService outpostService;
    private final PersonageService personageService;
    private final TopService topService;
    private final GroupTgService groupTgService;
    private final TelegramSender telegramSender;

    public OutpostDonateItemExecutor(
        UserService userService,
        OutpostService outpostService,
        PersonageService personageService,
        TopService topService,
        GroupTgService groupTgService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.outpostService = outpostService;
        this.personageService = personageService;
        this.topService = topService;
        this.groupTgService = groupTgService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(OutpostDonateItem command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var language = user.language();
        final var result = outpostService.tryDonateItemToBuilding(
            user.personageId(),
            command.building(),
            command.itemId()
        );
        result.peekLeft(error -> telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(OutpostLocalization.donateItemError(language, error))
            .keyboard(ReplyKeyboards.mainKeyboard(language))
            .build()
        ));
        result.peek(success -> {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(OutpostLocalization.donateItemSuccessPrivate(language, success))
                .keyboard(ReplyKeyboards.mainKeyboard(language))
                .build()
            );
            if (success.completed()) {
                final var groupIdOpt = personageService.getByIdForce(user.personageId()).memberGroupId();
                if (groupIdOpt.isEmpty()) {
                    return;
                }
                final var groupTg = groupTgService.forceGet(groupIdOpt.get());
                final var groupText = OutpostLocalization.groupBuildingCompletedWithTop(
                    groupTg.language(),
                    command.building(),
                    success.newLevel(),
                    topService.getTopOutpostBuildingMaterials(success.topContributors())
                );
                telegramSender.send(SendMessageBuilder.builder()
                    .chatId(groupTg.id())
                    .text(groupText)
                    .build()
                );
            }
        });
    }
}
