package ru.homyakin.seeker.telegram.command.user.shop;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.game.shop.models.ShopItemType;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.UserCommand;
import ru.homyakin.seeker.telegram.user.models.UserId;

import java.util.Optional;

public record BuyItem(
    UserId userId,
    Optional<ShopItemType> type,
    Optional<Integer> objectId
) implements UserCommand {
    public static BuyItem from(Message message) {
        final var parts = message.getText().split(TextConstants.TG_COMMAND_DELIMITER);
        final var suffix = parts.length > 1 ? parts[1] : "";
        final var userId = UserId.from(message.getFrom().getId());
        try {
            return new BuyItem(userId, Optional.empty(), Optional.of(Integer.parseInt(suffix)));
        } catch (NumberFormatException _) {
            return new BuyItem(userId, ShopItemType.findByCode(suffix), Optional.empty());
        }
    }
}
