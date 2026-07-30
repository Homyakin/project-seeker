package ru.homyakin.seeker.telegram.command.user.shop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.shop.EnhanceService;
import ru.homyakin.seeker.game.shop.errors.StormEnhanceError;
import ru.homyakin.seeker.game.shop.models.StormEnhanceOutcome;
import ru.homyakin.seeker.locale.shop.ShopLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class ConfirmStormEnhanceExecutor extends CommandExecutor<ConfirmStormEnhance> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final EnhanceService enhanceService;

    public ConfirmStormEnhanceExecutor(
        UserService userService,
        TelegramSender telegramSender,
        EnhanceService enhanceService
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.enhanceService = enhanceService;
    }

    @Override
    public void execute(ConfirmStormEnhance command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var text = enhanceService.stormEnhance(user.personageId(), command.itemId())
            .fold(
                error -> switch (error) {
                    case StormEnhanceError.NoSuchItem _ -> ShopLocalization.noItemAtPersonage(user.language());
                    case StormEnhanceError.MaxLevel _ -> ShopLocalization.maxStormEnhance(user.language());
                    case StormEnhanceError.NotEnoughStormShards notEnough ->
                        ShopLocalization.notEnoughStormShards(user.language(), notEnough.required());
                },
                result -> switch (result.outcome()) {
                    case StormEnhanceOutcome.SUCCESS ->
                        ShopLocalization.successStormEnhance(user.language(), result.action());
                    case StormEnhanceOutcome.FAILURE ->
                        ShopLocalization.failedStormEnhance(user.language(), result.action());
                }
            );
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .build()
        );
    }
}
