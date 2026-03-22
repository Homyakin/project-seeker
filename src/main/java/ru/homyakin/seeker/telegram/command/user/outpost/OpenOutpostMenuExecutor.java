package ru.homyakin.seeker.telegram.command.user.outpost;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.outpost.action.OutpostService;
import ru.homyakin.seeker.locale.outpost.OutpostLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.state.UserStateService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class OpenOutpostMenuExecutor extends CommandExecutor<OpenOutpostMenu> {
    private final UserService userService;
    private final UserStateService userStateService;
    private final TelegramSender telegramSender;
    private final GroupPersonageStorage groupPersonageStorage;
    private final OutpostService outpostService;

    public OpenOutpostMenuExecutor(
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
    public void execute(OpenOutpostMenu command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        userStateService.clearUserState(user);
        final var membership = groupPersonageStorage.getPersonageMemberGroup(user.personageId());
        if (membership.groupId().isEmpty()) {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(OutpostLocalization.outpostNoGroup(user.language()))
                .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
                .build()
            );
            return;
        }
        final var slots = outpostService.listSlots(membership.groupId().get());
        final var text = OutpostLocalization.outpost(user.language(), slots);
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
            .build()
        );
    }
}
