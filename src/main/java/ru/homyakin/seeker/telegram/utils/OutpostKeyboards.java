package ru.homyakin.seeker.telegram.utils;

import java.util.List;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.game.outpost.entity.OutpostBuildOffer;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.outpost.OutpostLocalization;
import ru.homyakin.seeker.telegram.TelegramBotConfig;
import ru.homyakin.seeker.telegram.command.type.CommandType;

public final class OutpostKeyboards {
    private OutpostKeyboards() {
    }

    public static InlineKeyboardMarkup openOutpostInPrivateKeyboard(Language language) {
        final var url = "https://t.me/%s?start=%s".formatted(
            TelegramBotConfig.username(),
            CommandType.OPEN_OUTPOST_MENU.getText()
        );
        return InlineKeyboardBuilder.builder()
            .addRow()
            .addUrlButton(OutpostLocalization.openInPrivateButton(language), url)
            .build();
    }

    public static InlineKeyboardMarkup outpostPrivateStartBuildingRow(Language language) {
        return InlineKeyboardBuilder.builder()
            .addRow()
            .addButton(
                OutpostLocalization.startBuildingButton(language),
                CommandType.OUTPOST_BUILD_PICKER_OPEN.getText()
            )
            .build();
    }

    public static InlineKeyboardMarkup outpostBuildingChoiceKeyboard(Language language, List<OutpostBuildOffer> offers) {
        var builder = InlineKeyboardBuilder.builder();
        for (final var offer : offers) {
            builder = builder.addRow().addButton(
                OutpostLocalization.buildingButtonLabel(
                    language,
                    offer.building(),
                    offer.fromLevel(),
                    offer.toLevel()
                ),
                CommandType.OUTPOST_BUILD_SELECT_BUILDING.getText()
                    + TextConstants.CALLBACK_DELIMITER + offer.building().id()
            );
        }
        return builder.build();
    }

    public static InlineKeyboardMarkup outpostConfirmStartKeyboard(Language language, Building building, int fromLevel) {
        final var confirmLabel = fromLevel == 0
            ? OutpostLocalization.confirmStartButton(language)
            : OutpostLocalization.confirmUpgradeButton(language);
        return InlineKeyboardBuilder.builder()
            .addRow()
            .addButton(
                OutpostLocalization.cancelStartButton(language),
                CommandType.OUTPOST_BUILD_CANCEL.getText()
            )
            .addButton(
                confirmLabel,
                CommandType.OUTPOST_BUILD_CONFIRM.getText()
                    + TextConstants.CALLBACK_DELIMITER + building.id()
            )
            .build();
    }

    public static InlineKeyboardMarkup emptyInlineKeyboard() {
        return new InlineKeyboardMarkup(List.of());
    }
}
