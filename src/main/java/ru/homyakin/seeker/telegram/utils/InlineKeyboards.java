package ru.homyakin.seeker.telegram.utils;

import java.util.List;
import net.fellbaum.jemoji.EmojiManager;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.personage.badge.PersonageAvailableBadge;
import ru.homyakin.seeker.game.personage.models.CharacteristicType;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItem;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.locale.help.HelpLocalization;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.locale.personal.CharacteristicLocalization;
import ru.homyakin.seeker.locale.raid.RaidLocalization;
import ru.homyakin.seeker.telegram.command.common.help.HelpSection;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.locale.Language;

public class InlineKeyboards {
    private static final String selectedIcon = EmojiManager.getByAlias(":white_check_mark:").orElseThrow().getEmoji();

    public static InlineKeyboardMarkup languageKeyboard(Language currentLanguage) {
        final var languages = Language.values();
        final var builder = InlineKeyboardBuilder.builder();
        for (int i = 0; i < languages.length; ++i) {
            if (i % 5 == 0) {
                builder.addRow();
            }
            final String text;
            if (currentLanguage == languages[i]) {
                text = selectedIcon + languages[i].buttonText();
            } else {
                text = languages[i].buttonText();
            }
            builder.addButton(
                text,
                CommandType.SELECT_LANGUAGE.getText() + TextConstants.CALLBACK_DELIMITER + languages[i].id()
            );
        }
        return builder.build();
    }

    public static InlineKeyboardMarkup joinRaidEventKeyboard(Language language, long chatEventId) {
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                RaidLocalization.joinRaidEvent(language),
                CommandType.JOIN_EVENT.getText() + TextConstants.CALLBACK_DELIMITER + chatEventId
            )
            .build();
    }

    public static InlineKeyboardMarkup duelKeyboard(Language language, long duelId) {
        final var callbackPostfix = TextConstants.CALLBACK_DELIMITER + duelId;
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                DuelLocalization.declineDuelButton(language), CommandType.DECLINE_DUEL.getText() + callbackPostfix
            )
            .addButton(
                DuelLocalization.acceptDuelButton(language), CommandType.ACCEPT_DUEL.getText() + callbackPostfix
            )
            .build();
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
            .addButton(HelpLocalization.battleSystemButton(language), callbackPrefix + HelpSection.BATTLE_SYSTEM.name())
            .addRow()
            .addButton(HelpLocalization.infoButton(language), callbackPrefix + HelpSection.INFO.name())
            .build();
    }

    public static InlineKeyboardMarkup resetCharacteristicsConfirmationKeyboard(Language language) {
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(CharacteristicLocalization.cancelButton(language), CommandType.CANCEL_RESET_CHARACTERISTICS.getText())
            .addButton(CharacteristicLocalization.confirmButton(language), CommandType.CONFIRM_RESET_CHARACTERISTICS.getText())
            .build();
    }

    public static InlineKeyboardMarkup chooseCharacteristicsKeyboard(Language language) {
        final var callbackPrefix = CommandType.INCREASE_CHARACTERISTIC.getText() + TextConstants.CALLBACK_DELIMITER;
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(CharacteristicLocalization.strengthButton(language), callbackPrefix + CharacteristicType.STRENGTH.name())
            .addButton(CharacteristicLocalization.agilityButton(language), callbackPrefix + CharacteristicType.AGILITY.name())
            .addButton(CharacteristicLocalization.wisdomButton(language), callbackPrefix + CharacteristicType.WISDOM.name())
            .build();
    }

    public static InlineKeyboardMarkup badgeSelector(List<PersonageAvailableBadge> badges) {
        final var builder = InlineKeyboardBuilder.builder();
        final var callbackPrefix = CommandType.SELECT_BADGE.getText() + TextConstants.CALLBACK_DELIMITER;
        for (int i = 0; i < badges.size(); ++i) {
            if (i % 4 == 0) { // по 4 элемента в строке
                builder.addRow();
            }
            final var badge = badges.get(i);
            if (badge.isActive()) {
                builder.addButton(selectedIcon + badge.badge().view().icon(), callbackPrefix + badge.badge().id());
            } else {
                builder.addButton(badge.badge().view().icon(), callbackPrefix + badge.badge().id());
            }

        }
        return builder.build();
    }

    public static InlineKeyboardMarkup consumeMenuItemOrderKeyboard(
        Language language,
        long orderId,
        MenuItem menuItem
    ) {
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                menuItem.category().consumeButtonText(language),
                CommandType.CONSUME_MENU_ITEM_ORDER.getText() + TextConstants.CALLBACK_DELIMITER + orderId
            )
            .build();
    }

    public static InlineKeyboardMarkup confirmDropItemKeyboard(Language language, Item item) {
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                ItemLocalization.rejectDropButton(language),
                CommandType.REJECT_DROP_ITEM.getText()
            )
            .addButton(
                ItemLocalization.confirmDropButton(language),
                item.confirmDropCommand()
            )
            .build();
    }
}
