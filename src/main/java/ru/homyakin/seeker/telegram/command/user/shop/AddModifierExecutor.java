package ru.homyakin.seeker.telegram.command.user.shop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.shop.EnhanceService;
import ru.homyakin.seeker.game.shop.errors.AddModifierError;
import ru.homyakin.seeker.locale.shop.ShopLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class AddModifierExecutor extends CommandExecutor<AddModifier> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final EnhanceService enhanceService;

    public AddModifierExecutor(UserService userService, TelegramSender telegramSender, EnhanceService enhanceService) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.enhanceService = enhanceService;
    }

    @Override
    public void execute(AddModifier command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var text = enhanceService.addModifier(user.personageId(), command.itemId())
            .fold(
                error -> switch (error) {
                    case AddModifierError.NoSuchItem _ -> ShopLocalization.noItemAtPersonage(user.language());
                    case AddModifierError.MaxModifiers _ -> ShopLocalization.maxModifiers(user.language());
                    case AddModifierError.NotEnoughMoney notEnoughMoney ->
                        ShopLocalization.notEnoughMoney(user.language(), notEnoughMoney.required());
                },
                action -> ShopLocalization.successAddModifier(user.language(), action)
            );
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .build()
        );
    }
}
