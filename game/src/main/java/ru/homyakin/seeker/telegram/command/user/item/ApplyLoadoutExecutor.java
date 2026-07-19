package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class ApplyLoadoutExecutor extends CommandExecutor<ApplyLoadout> {
    private final UserService userService;
    private final EquipmentLoadoutService loadoutService;
    private final LoadoutMessageService loadoutMessageService;
    private final TelegramSender telegramSender;

    public ApplyLoadoutExecutor(
        UserService userService,
        EquipmentLoadoutService loadoutService,
        LoadoutMessageService loadoutMessageService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.loadoutService = loadoutService;
        this.loadoutMessageService = loadoutMessageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(ApplyLoadout command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var loadout = loadoutService.get(user.personageId(), command.loadoutId());
        if (loadout.isEmpty()) {
            telegramSender.send(TelegramMethods.createAnswerCallbackQuery(
                command.callbackId(),
                ItemLocalization.loadoutNotFound(user.language())
            ));
            loadoutMessageService.editLoadoutsList(user, command.messageId());
            return;
        }
        final var result = loadoutService.apply(user.personageId(), command.loadoutId());
        final var text = result.fold(
            error -> ItemLocalization.applyLoadoutError(user.language(), error),
            _ -> ItemLocalization.successApplyLoadout(user.language(), loadout.get().name())
        );
        telegramSender.send(TelegramMethods.createAnswerCallbackQuery(command.callbackId(), text));
        loadoutService.get(user.personageId(), command.loadoutId())
            .ifPresent(updated -> loadoutMessageService.editLoadoutDetail(user, command.messageId(), updated));
    }
}
