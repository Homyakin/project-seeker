package ru.homyakin.seeker.telegram.command.user.outpost;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.outpost.action.OutpostService;
import ru.homyakin.seeker.locale.outpost.OutpostLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.OutpostKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class OutpostOpenBuildPickerExecutor extends CommandExecutor<OutpostOpenBuildPicker> {
    private final UserService userService;
    private final OutpostService outpostService;
    private final TelegramSender telegramSender;

    public OutpostOpenBuildPickerExecutor(
        UserService userService,
        OutpostService outpostService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.outpostService = outpostService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(OutpostOpenBuildPicker command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var groupIdOpt = outpostService.groupIdIfPersonageCanBuild(user.personageId());
        if (groupIdOpt.isEmpty()) {
            telegramSender.send(TelegramMethods.createAnswerCallbackQuery(
                command.callbackId(),
                OutpostLocalization.notAdminOutpost(user.language())
            ));
            return;
        }
        final var groupId = groupIdOpt.get();
        final var available = outpostService.listBuildOffers(groupId);
        if (available.isEmpty()) {
            telegramSender.send(TelegramMethods.createAnswerCallbackQuery(
                command.callbackId(),
                OutpostLocalization.startBuildingConflict(user.language())
            ));
            return;
        }
        telegramSender.send(EditMessageTextBuilder.builder()
            .chatId(user.id())
            .messageId(command.messageId())
            .text(OutpostLocalization.chooseBuildingPicker(user.language(), available))
            .keyboard(OutpostKeyboards.outpostBuildingChoiceKeyboard(user.language(), available))
            .build()
        );
    }
}
