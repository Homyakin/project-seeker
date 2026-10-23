package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.PlayerCombatConfig;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.errors.TakeOffItemError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class TakeOffItemExecutor extends CommandExecutor<TakeOffItem> {
    private final UserService userService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;
    private final ItemService itemService;
    private final PlayerCombatConfig playerCombatConfig;

    public TakeOffItemExecutor(
        UserService userService,
        PersonageService personageService,
        TelegramSender telegramSender,
        ItemService itemService,
        PlayerCombatConfig playerCombatConfig
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
        this.itemService = itemService;
        this.playerCombatConfig = playerCombatConfig;
    }

    @Override
    public void execute(TakeOffItem command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var text = itemService.takeOffItem(personageService.getByIdForce(user.personageId()), command.itemId())
            .fold(
                error -> switch (error) {
                    case TakeOffItemError.PersonageMissingItem _ -> ItemLocalization.personageMissingItem(user.language());
                    case TakeOffItemError.AlreadyTakenOff _ -> ItemLocalization.alreadyTakenOff(user.language());
                    case TakeOffItemError.NotEnoughSpaceInBag _ -> ItemLocalization.notEnoughSpaceInBag(user.language());
                },
                item -> ItemLocalization.successTakeOff(user.language(), item, playerCombatConfig.version())
            );
        telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(user.id())
                .text(text)
                .build()
        );
    }
}
