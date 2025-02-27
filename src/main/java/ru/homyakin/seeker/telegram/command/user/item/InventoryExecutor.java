package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class InventoryExecutor extends CommandExecutor<Inventory> {
    private final UserService userService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;
    private final ItemService itemService;

    public InventoryExecutor(
        UserService userService,
        PersonageService personageService,
        TelegramSender telegramSender,
        ItemService itemService
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
        this.itemService = itemService;
    }

    @Override
    public void execute(Inventory command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var items = itemService.getPersonageItems(user.personageId());
        telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(user.id())
                .text(ItemLocalization.inventory(user.language(), personageService.getByIdForce(user.personageId()), items))
                .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
                .build()
        );
    }
}
