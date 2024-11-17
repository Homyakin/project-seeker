package ru.homyakin.seeker.telegram.command.user.item.drop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.errors.DropItemError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;

@Component
public class ConfirmDropItemExecutor extends CommandExecutor<ConfirmDropItem> {
    private final UserService userService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;
    private final ItemService itemService;

    public ConfirmDropItemExecutor(
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
    public void execute(ConfirmDropItem command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var text = itemService.dropItem(personageService.getByIdForce(user.personageId()), command.itemId())
            .fold(
                error -> switch (error) {
                    case DropItemError.PersonageMissingItem _ ->
                        ItemLocalization.personageMissingItem(user.language());
                },
                item -> ItemLocalization.successDrop(user.language(), item)
            );
        telegramSender.send(
            EditMessageTextBuilder.builder()
                .text(text)
                .chatId(user.id())
                .messageId(command.messageId())
                .build()
        );
    }
}
