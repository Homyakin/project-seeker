package ru.homyakin.seeker.telegram.command.user.item.drop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.models.DropItemError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class DropItemExecutor extends CommandExecutor<DropItem> {
    private final UserService userService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;
    private final ItemService itemService;

    public DropItemExecutor(
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
    public void execute(DropItem command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        final var message = itemService.canDropItem(personageService.getByIdForce(user.personageId()), command.itemId())
            .fold(
                error -> {
                    final var text = switch (error) {
                        case DropItemError.PersonageMissingItem _ ->
                            ItemLocalization.personageMissingItem(user.language());
                    };
                    return SendMessageBuilder.builder()
                        .chatId(user.id())
                        .text(text)
                        .build();
                },
                item -> SendMessageBuilder.builder()
                    .chatId(user.id())
                    .text(ItemLocalization.confirmDrop(user.language(), item))
                    .keyboard(InlineKeyboards.confirmDropItemKeyboard(user.language(), item))
                    .build()
            );
        telegramSender.send(message);
    }
}
