package ru.homyakin.seeker.telegram.command.user.shop;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.game.shop.models.ShopItemType;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.Command;
import ru.homyakin.seeker.telegram.user.models.UserId;

import java.util.Optional;

public record BuyItem(UserId userId, Optional<ShopItemType> type) implements Command {
    public static BuyItem from(Message message) {
        final var code = message.getText().split(TextConstants.TG_COMMAND_DELIMITER)[1];
        return new BuyItem(
            UserId.from(message.getFrom().getId()),
            ShopItemType.findByCode(code)
        );
    }
}
