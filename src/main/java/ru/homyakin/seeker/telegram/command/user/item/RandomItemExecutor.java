package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class RandomItemExecutor extends CommandExecutor<RandomItem> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final ItemService itemService;

    public RandomItemExecutor(
        UserService userService,
        TelegramSender telegramSender,
        ItemService itemService
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.itemService = itemService;
    }

    @Override
    public void execute(RandomItem command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        final var item = itemService.generateItemForPersonage(user.personageId());
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(item.toString() + "\n\n" + ItemLocalization.fullItem(user.language(), item))
            .build()
        );
    }
}
