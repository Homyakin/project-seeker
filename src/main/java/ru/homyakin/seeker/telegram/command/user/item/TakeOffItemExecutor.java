package ru.homyakin.seeker.telegram.command.user.item;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.LegacyItemService;
import ru.homyakin.seeker.game.item.errors.LegacyTakeOffItemError;
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
    private final LegacyItemService itemService;

    public TakeOffItemExecutor(
        UserService userService,
        PersonageService personageService,
        TelegramSender telegramSender,
        LegacyItemService itemService
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
        this.itemService = itemService;
    }

    @Override
    public void execute(TakeOffItem command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var text = itemService.takeOffItem(personageService.getByIdForce(user.personageId()), command.itemId())
            .fold(
                error -> switch (error) {
                    case LegacyTakeOffItemError.PersonageMissingItem _ -> ItemLocalization.personageMissingItem(user.language());
                    case LegacyTakeOffItemError.AlreadyTakenOff _ -> ItemLocalization.alreadyTakenOff(user.language());
                    case LegacyTakeOffItemError.NotEnoughSpaceInBag _ -> ItemLocalization.notEnoughSpaceInBag(user.language());
                },
                item -> ItemLocalization.successTakeOff(user.language(), item)
            );
        telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(user.id())
                .text(text)
                .build()
        );
    }
}
