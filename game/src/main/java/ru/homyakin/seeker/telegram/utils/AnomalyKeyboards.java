package ru.homyakin.seeker.telegram.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.homyakin.seeker.common.models.GroupId;
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
        Anomaly anomaly,
        GroupId viewerGroupId
    ) {
        return switch (anomaly) {
            case Anomaly.Safe safe when safe.phase() == Anomaly.Safe.Phase.GATHERING ->
                gatheringKeyboard(language, launchedEventId);
            case Anomaly.Dangerous.Gathering _ ->
                gatheringKeyboard(language, launchedEventId);
            case Anomaly.Dangerous.Challenged challenged
                when challenged.opponentGroupId().equals(viewerGroupId) ->
                gatheringKeyboard(language, launchedEventId);
            case Anomaly.Dangerous.Accepted accepted
                when accepted.opponentGroupId().equals(viewerGroupId)
                    && accepted.winnerGroupId().isEmpty() ->
                gatheringKeyboard(language, launchedEventId);
            case Anomaly.Safe _, Anomaly.Dangerous.Searching _, Anomaly.Dangerous.Challenged _,
                 Anomaly.Dangerous.Accepted _ ->
                OutpostKeyboards.emptyInlineKeyboard();
        };
    }
}

