package ru.homyakin.seeker.telegram.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.homyakin.seeker.game.event.anomaly.entity.Anomaly;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.anomaly.AnomalyLocalization;
import ru.homyakin.seeker.telegram.command.type.CommandType;

public final class AnomalyKeyboards {
    private AnomalyKeyboards() {
    }

    public static InlineKeyboardMarkup menuKeyboard(Language language, boolean canStart) {
        var builder = InlineKeyboardBuilder.builder();
        if (canStart) {
            builder = builder.addRow()
                .addButton(
                    AnomalyLocalization.safeModeButton(language),
                    CommandType.ANOMALY_CHOOSE_SAFE.getText()
                )
                .addButton(
                    AnomalyLocalization.dangerousModeButton(language),
                    CommandType.ANOMALY_CHOOSE_DANGEROUS.getText()
                );
        }
        return builder.addRow()
            .addButton(
                AnomalyLocalization.backToOutpostButton(language),
                CommandType.ANOMALY_BACK_OUTPOST.getText()
            )
            .build();
    }

    public static InlineKeyboardMarkup gatheringKeyboard(Language language, long launchedEventId) {
        final var id = TextConstants.CALLBACK_DELIMITER + launchedEventId;
        return InlineKeyboardBuilder.builder()
            .addRow()
            .addButton(
                AnomalyLocalization.joinButton(language),
                CommandType.ANOMALY_JOIN.getText() + id
            )
            .addRow()
            .addButton(
                AnomalyLocalization.readyButton(language),
                CommandType.ANOMALY_READY.getText() + id
            )
            .build();
    }

    public static InlineKeyboardMarkup forEvent(
        Language language,
        long launchedEventId,
        Anomaly anomaly
    ) {
        return switch (anomaly) {
            case Anomaly.Safe safe when safe.phase() == Anomaly.Safe.Phase.GATHERING && !safe.rosterLocked() ->
                gatheringKeyboard(language, launchedEventId);
            case Anomaly.Dangerous dangerous
                when dangerous.phase() == Anomaly.Dangerous.Phase.GATHERING && !dangerous.rosterLocked() ->
                gatheringKeyboard(language, launchedEventId);
            case Anomaly.Challenged challenged when !challenged.rosterLocked() ->
                gatheringKeyboard(language, launchedEventId);
            case Anomaly.Safe _, Anomaly.Dangerous _, Anomaly.Challenged _ ->
                OutpostKeyboards.emptyInlineKeyboard();
        };
    }
}
