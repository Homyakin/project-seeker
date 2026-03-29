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
public class OutpostSelectStartBuildingExecutor extends CommandExecutor<OutpostSelectStartBuilding> {
    private final UserService userService;
    private final OutpostService outpostService;
    private final TelegramSender telegramSender;

    public OutpostSelectStartBuildingExecutor(
        UserService userService,
        OutpostService outpostService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.outpostService = outpostService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(OutpostSelectStartBuilding command) {
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
        final var building = command.building();
        final var offerOpt = outpostService.listBuildOffers(groupId).stream()
            .filter(o -> o.building() == building)
            .findFirst();
        if (offerOpt.isEmpty()) {
            telegramSender.send(TelegramMethods.createAnswerCallbackQuery(
                command.callbackId(),
                OutpostLocalization.startBuildingConflict(user.language())
            ));
            return;
        }
        final var fromLevel = offerOpt.get().fromLevel();
        final var toLevel = offerOpt.get().toLevel();
        telegramSender.send(EditMessageTextBuilder.builder()
            .chatId(user.id())
            .messageId(command.messageId())
            .text(OutpostLocalization.confirmStartBuilding(
                user.language(),
                building,
                fromLevel,
                toLevel,
                building.materialsToReachLevel(toLevel)
            ))
            .keyboard(OutpostKeyboards.outpostConfirmStartKeyboard(user.language(), building, fromLevel))
            .build()
        );
    }
}
