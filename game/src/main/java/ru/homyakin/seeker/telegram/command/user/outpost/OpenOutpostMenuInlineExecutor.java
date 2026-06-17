package ru.homyakin.seeker.telegram.command.user.outpost;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.outpost.action.OutpostService;
import ru.homyakin.seeker.locale.outpost.OutpostLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.state.UserStateService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.OutpostKeyboards;

@Component
public class OpenOutpostMenuInlineExecutor extends CommandExecutor<OpenOutpostMenuInline> {
    private final UserService userService;
    private final UserStateService userStateService;
    private final TelegramSender telegramSender;
    private final GroupPersonageStorage groupPersonageStorage;
    private final OutpostService outpostService;

    public OpenOutpostMenuInlineExecutor(
        UserService userService,
        UserStateService userStateService,
        TelegramSender telegramSender,
        GroupPersonageStorage groupPersonageStorage,
        OutpostService outpostService
    ) {
        this.userService = userService;
        this.userStateService = userStateService;
        this.telegramSender = telegramSender;
        this.groupPersonageStorage = groupPersonageStorage;
        this.outpostService = outpostService;
    }

    @Override
    public void execute(OpenOutpostMenuInline command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        userStateService.clearUserState(user);
        final var membership = groupPersonageStorage.getPersonageMemberGroup(user.personageId());
        if (membership.groupId().isEmpty()) {
            telegramSender.send(EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(command.messageId())
                .text(OutpostLocalization.outpostNoGroup(user.language()))
                .keyboard(OutpostKeyboards.emptyInlineKeyboard())
                .build()
            );
            return;
        }
        final var groupId = membership.groupId().get();
        final var slots = outpostService.listSlots(groupId);
        final var text = OutpostLocalization.outpost(user.language(), slots, true);
        final var canOfferStart = outpostService.canPersonageBuild(user.personageId())
            && !outpostService.listBuildOffers(groupId).isEmpty();
        final var keyboard = canOfferStart
            ? OutpostKeyboards.outpostPrivateStartBuildingRow(user.language())
            : OutpostKeyboards.emptyInlineKeyboard();
        telegramSender.send(EditMessageTextBuilder.builder()
            .chatId(user.id())
            .messageId(command.messageId())
            .text(text)
            .keyboard(keyboard)
            .build()
        );
    }
}
