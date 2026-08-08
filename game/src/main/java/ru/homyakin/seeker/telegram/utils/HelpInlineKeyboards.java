package ru.homyakin.seeker.telegram.utils;

import java.util.EnumSet;
import java.util.Set;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.help.HelpLocalization;
import ru.homyakin.seeker.locale.personal.MenuLocalization;
import ru.homyakin.seeker.telegram.command.common.help.HelpSection;
import ru.homyakin.seeker.telegram.command.common.help.SelectHelp;
import ru.homyakin.seeker.telegram.command.type.CommandType;

public final class HelpInlineKeyboards {
    private HelpInlineKeyboards() {
    }

    public static InlineKeyboardMarkup helpKeyboard(Language language) {
        final var callbackPrefix = CommandType.SELECT_HELP.getText() + TextConstants.CALLBACK_DELIMITER;
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(HelpLocalization.raidsButton(language), callbackPrefix + HelpSection.RAIDS.name())
            .addButton(HelpLocalization.duelsButton(language), callbackPrefix + HelpSection.DUELS.name())
            .addRow()
            .addButton(HelpLocalization.menuButton(language), callbackPrefix + HelpSection.MENU.name())
            .addButton(HelpLocalization.personageButton(language), callbackPrefix + HelpSection.PERSONAGE.name())
            .addRow()
            .addButton(HelpLocalization.seasonsButton(language), callbackPrefix + HelpSection.SEASONS.name())
            .addButton(HelpLocalization.battleSystemButton(language), callbackPrefix + HelpSection.BATTLE_SYSTEM.name())
            .addRow()
            .addButton(HelpLocalization.infoButton(language), callbackPrefix + HelpSection.INFO.name())
            .build();
    }

    public static InlineKeyboardMarkup battleHelpKeyboard(Language language) {
        final var callbackPrefix = CommandType.SELECT_HELP.getText() + TextConstants.CALLBACK_DELIMITER;
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                HelpLocalization.battleGeneralButton(language),
                callbackPrefix + HelpSection.BATTLE_GENERAL.name()
            )
            .addRow()
            .addButton(
                HelpLocalization.battleMatrixButton(language),
                callbackPrefix + HelpSection.BATTLE_MATRIX.name()
            )
            .addRow()
            .addButton(
                HelpLocalization.battleSkillsButton(language),
                battleSkillsCallback(0, Set.of())
            )
            .addRow()
            .addButton(MenuLocalization.backButton(language), callbackPrefix + HelpSection.MAIN.name())
            .build();
    }

    public static InlineKeyboardMarkup battleSkillsHelpKeyboard(
        Language language,
        int page,
        int totalPages,
        Set<PersonageSlot> slotFilters
    ) {
        final var builder = InlineKeyboardBuilder.builder();
        builder.addRow()
            .addButton(
                HelpLocalization.battleSkillsAllFilterButton(language),
                battleSkillsCallback(0, Set.of()),
                slotFilters.isEmpty() ? InlineButtonStyle.SUCCESS : null
            );
        final var slots = PersonageSlot.values();
        for (int i = 0; i < slots.length; ++i) {
            if (i % 4 == 0) {
                builder.addRow();
            }
            final var slot = slots[i];
            final var isActive = slotFilters.contains(slot);
            builder.addButton(
                slot.icon,
                battleSkillsCallback(0, toggledSlotFilters(slotFilters, slot)),
                isActive ? InlineButtonStyle.SUCCESS : null
            );
        }
        if (totalPages > 1) {
            builder.addRow();
            if (page > 0) {
                builder.addButton(
                    HelpLocalization.battleSkillsPrevButton(language),
                    battleSkillsCallback(page - 1, slotFilters)
                );
            }
            if (page < totalPages - 1) {
                builder.addButton(
                    HelpLocalization.battleSkillsNextButton(language),
                    battleSkillsCallback(page + 1, slotFilters)
                );
            }
        }
        builder.addRow()
            .addButton(
                MenuLocalization.backButton(language),
                CommandType.SELECT_HELP.getText()
                    + TextConstants.CALLBACK_DELIMITER
                    + HelpSection.BATTLE_SYSTEM.name()
            );
        return builder.build();
    }

    private static Set<PersonageSlot> toggledSlotFilters(Set<PersonageSlot> current, PersonageSlot slot) {
        final var next = EnumSet.noneOf(PersonageSlot.class);
        next.addAll(current);
        if (!next.add(slot)) {
            next.remove(slot);
        }
        return Set.copyOf(next);
    }

    private static String battleSkillsCallback(int page, Set<PersonageSlot> slotFilters) {
        return CommandType.SELECT_HELP.getText()
            + TextConstants.CALLBACK_DELIMITER
            + HelpSection.BATTLE_SKILLS.name()
            + TextConstants.CALLBACK_DELIMITER
            + page
            + TextConstants.CALLBACK_DELIMITER
            + SelectHelp.encodeSlotFilters(slotFilters);
    }
}
