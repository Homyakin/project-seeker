package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.game.item.loadout.entity.CreateLoadoutError;
import ru.homyakin.seeker.game.item.loadout.entity.RenameLoadoutError;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.user.state.LoadoutNameState;
import ru.homyakin.seeker.telegram.user.state.UserStateService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class InputLoadoutNameExecutor extends CommandExecutor<InputLoadoutName> {
    private final UserService userService;
    private final UserStateService userStateService;
    private final EquipmentLoadoutService loadoutService;
    private final LoadoutMessageService loadoutMessageService;
    private final TelegramSender telegramSender;

    public InputLoadoutNameExecutor(
        UserService userService,
        UserStateService userStateService,
        EquipmentLoadoutService loadoutService,
        LoadoutMessageService loadoutMessageService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.userStateService = userStateService;
        this.loadoutService = loadoutService;
        this.loadoutMessageService = loadoutMessageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(InputLoadoutName command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var state = userStateService.getUserStateById(user.id()).orElse(null);
        if (!(state instanceof LoadoutNameState loadoutNameState)) {
            return;
        }
        switch (loadoutNameState) {
            case LoadoutNameState.CreateLoadout create -> handleCreate(user, create.messageId(), command.name());
            case LoadoutNameState.RenameLoadout rename -> handleRename(
                user,
                rename.loadoutId(),
                rename.messageId(),
                command.name()
            );
        }
    }

    private void handleCreate(User user, int messageId, String name) {
        final var result = loadoutService.createFromCurrent(user.personageId(), name);
        if (result.isLeft()) {
            final var text = switch (result.getLeft()) {
                case CreateLoadoutError.InvalidName invalid ->
                    ItemLocalization.loadoutNameError(user.language(), invalid.nameError());
                case CreateLoadoutError.MaxLoadoutsReached _ ->
                    ItemLocalization.maxLoadoutsReached(user.language());
            };
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(text)
                .build()
            );
            return;
        }
        userStateService.clearUserState(user);
        final var loadout = result.get();
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(ItemLocalization.successCreateLoadout(user.language(), loadout.name()))
            .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
            .build()
        );
        loadoutMessageService.editLoadoutDetail(user, messageId, loadout);
    }

    private void handleRename(User user, long loadoutId, int messageId, String name) {
        final var result = loadoutService.rename(user.personageId(), loadoutId, name);
        if (result.isLeft()) {
            final var text = switch (result.getLeft()) {
                case RenameLoadoutError.InvalidName invalid ->
                    ItemLocalization.loadoutNameError(user.language(), invalid.nameError());
                case RenameLoadoutError.LoadoutNotFound _ ->
                    ItemLocalization.loadoutNotFound(user.language());
            };
            if (result.getLeft() instanceof RenameLoadoutError.LoadoutNotFound) {
                userStateService.clearUserState(user);
                telegramSender.send(SendMessageBuilder.builder()
                    .chatId(user.id())
                    .text(text)
                    .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
                    .build()
                );
                loadoutMessageService.editLoadoutsList(user, messageId);
                return;
            }
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(text)
                .keyboard(ReplyKeyboards.loadoutNameKeyboard(user.language()))
                .build()
            );
            return;
        }
        userStateService.clearUserState(user);
        final var loadout = result.get();
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(ItemLocalization.successRenameLoadout(user.language(), loadout.name()))
            .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
            .build()
        );
        loadoutMessageService.editLoadoutDetail(user, messageId, loadout);
    }
}
