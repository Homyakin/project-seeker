package ru.homyakin.seeker.telegram.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.homyakin.seeker.game.contraband.entity.Contraband;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.contraband.ContrabandLocalization;
import ru.homyakin.seeker.telegram.command.type.CommandType;

public class ContrabandKeyboards {

    public static InlineKeyboardMarkup finderChoiceKeyboard(
        Language language,
        Contraband contraband,
        int finderSuccessChancePercent,
        Money sellPrice
    ) {
        final var delimiter = TextConstants.CALLBACK_DELIMITER;
        return InlineKeyboardBuilder.builder()
            .addRow()
            .addButton(
                ContrabandLocalization.forceOpenButton(language, finderSuccessChancePercent),
                CommandType.FORCE_OPEN_CONTRABAND.getText() + delimiter + contraband.id()
            )
            .addRow()
            .addButton(
                ContrabandLocalization.sellToMarketButton(language, sellPrice),
                CommandType.SELL_TO_BLACK_MARKET.getText() + delimiter + contraband.id()
            )
            .build();
    }

    public static InlineKeyboardMarkup receiverOpenKeyboard(
        Language language,
        Contraband contraband,
        int receiverSuccessChancePercent
    ) {
        return InlineKeyboardBuilder.builder()
            .addRow()
            .addButton(
                ContrabandLocalization.openAsReceiverButton(language, receiverSuccessChancePercent),
                CommandType.OPEN_CONTRABAND_AS_RECEIVER.getText() + TextConstants.CALLBACK_DELIMITER + contraband.id()
            )
            .build();
    }
}
