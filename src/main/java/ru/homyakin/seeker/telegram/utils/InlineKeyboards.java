package ru.homyakin.seeker.telegram.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.locale.raid.RaidLocalization;
import ru.homyakin.seeker.telegram.command.CommandType;
import ru.homyakin.seeker.locale.Language;

public class InlineKeyboards {
    public static InlineKeyboardMarkup languageKeyboard(Language currentLanguage) {
        final var languages = Language.values();
        final var builder = InlineKeyboardBuilder.builder();
        for (int i = 0; i < languages.length; ++i) {
            if (i % 5 == 0) {
                builder.addRow();
            }
            final String text;
            if (currentLanguage == languages[i]) {
                text = ":white_check_mark:" + languages[i].value();
            } else {
                text = languages[i].value();
            }
            builder.addButton(
                text,
                "%s%s%d".formatted(CommandType.SELECT_LANGUAGE.getText(), CommandType.CALLBACK_DELIMITER, languages[i].id())
            );
        }
        return builder.build();
    }

    public static InlineKeyboardMarkup joinRaidEventKeyboard(Language language, long chatEventId) {
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                RaidLocalization.get(language).joinRaidEvent(),
                "%s%s%d".formatted(CommandType.JOIN_EVENT.getText(), CommandType.CALLBACK_DELIMITER, chatEventId)
            )
            .build();
    }

    public static InlineKeyboardMarkup duelKeyboard(Language language, long duelId) {
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                DuelLocalization.get(language).declineDuelButton(),
                "%s%s%d".formatted(CommandType.DECLINE_DUEL.getText(), CommandType.CALLBACK_DELIMITER, duelId)
            )
            .addButton(
                DuelLocalization.get(language).acceptDuelButton(),
                "%s%s%d".formatted(CommandType.ACCEPT_DUEL.getText(), CommandType.CALLBACK_DELIMITER, duelId)
            )
            .build();
    }
}
