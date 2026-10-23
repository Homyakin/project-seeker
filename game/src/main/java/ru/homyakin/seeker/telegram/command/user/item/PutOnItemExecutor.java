package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.PlayerCombatConfig;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.errors.PutOnItemError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class PutOnItemExecutor extends CommandExecutor<PutOnItem> {
    private final UserService userService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;
    private final ItemService itemService;
    private final PlayerCombatConfig playerCombatConfig;

    public PutOnItemExecutor(
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
    public void execute(PutOnItem command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var text = itemService.putOnItem(personageService.getByIdForce(user.personageId()), command.itemId())
            .fold(
                error -> switch (error) {
                    case PutOnItemError.PersonageMissingItem _ -> ItemLocalization.personageMissingItem(user.language());
                    case PutOnItemError.AlreadyEquipped _ -> ItemLocalization.alreadyEquipped(user.language());
                    case PutOnItemError.NotEnoughSpaceOnPutOnItem notEnoughSpaceOnPutOnItem ->
                        ItemLocalization.notEnoughSpaceOnPutOnItem(user.language(), notEnoughSpaceOnPutOnItem.slots());
                },
                result -> ItemLocalization.successPutOn(user.language(), result, playerCombatConfig.version())
            );
        telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(user.id())
                .text(text)
                .build()
        );
    }
}
