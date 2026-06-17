package ru.homyakin.seeker.telegram.command.user.outpost;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.outpost.action.OutpostService;
import ru.homyakin.seeker.game.outpost.entity.OutpostSlot;
import ru.homyakin.seeker.game.outpost.entity.OutpostSlotAccessError;
import ru.homyakin.seeker.locale.outpost.OutpostLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.state.UserStateService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.OutpostKeyboards;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class OpenOutpostBuildingExecutor extends CommandExecutor<OpenOutpostBuilding> {
    private final UserService userService;
    private final UserStateService userStateService;
    private final TelegramSender telegramSender;
    private final OutpostService outpostService;

    public OpenOutpostBuildingExecutor(
        UserService userService,
        UserStateService userStateService,
        TelegramSender telegramSender,
        OutpostService outpostService
    ) {
        this.userService = userService;
        this.userStateService = userStateService;
        this.telegramSender = telegramSender;
        this.outpostService = outpostService;
    }

    @Override
    public void execute(OpenOutpostBuilding command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        userStateService.clearUserState(user);
        final var language = user.language();
        final var building = command.building();

        final var slotResult = outpostService.slotForBuilding(user.personageId(), building);
        if (slotResult.isLeft() && slotResult.getLeft() == OutpostSlotAccessError.NoGroup.INSTANCE) {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(OutpostLocalization.outpostNoGroup(language))
                .keyboard(ReplyKeyboards.mainKeyboard(language))
                .build()
            );
            return;
        }
        final var slot = slotResult.isRight() ? slotResult.get() : OutpostSlot.EmptySlot.INSTANCE;
        final var text = OutpostLocalization.buildingMenu(language, building, slot);
        final var keyboard = OutpostKeyboards.outpostBuildingMainMenuKeyboard(language, building, slot);
        if (command.editMessageId().isPresent()) {
            telegramSender.send(EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(command.editMessageId().get())
                .text(text)
                .keyboard(keyboard)
                .build()
            );
        } else {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(text)
                .keyboard(keyboard)
                .build()
            );
        }
    }
}
