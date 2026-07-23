package ru.homyakin.seeker.telegram.utils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import net.fellbaum.jemoji.EmojiManager;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.group.entity.EventIntervals;
import ru.homyakin.seeker.game.badge.entity.AvailableBadge;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.game.item.loadout.entity.EquipmentLoadout;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.settings.entity.PersonageSetting;
import ru.homyakin.seeker.game.personage.settings.entity.PersonageSettings;
import ru.homyakin.seeker.game.tavern_menu.menu.models.MenuItem;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.battle.BattleLocalization;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.locale.group.GroupSettingsLocalization;
import ru.homyakin.seeker.locale.personal.MenuLocalization;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.locale.personal.SettingsLocalization;
import ru.homyakin.seeker.locale.outpost.OutpostLocalization;
import ru.homyakin.seeker.locale.shop.ShopLocalization;
import ru.homyakin.seeker.locale.raid.RaidLocalization;
import ru.homyakin.seeker.locale.world_raid.WorldRaidLocalization;
import ru.homyakin.seeker.telegram.TelegramBotConfig;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.command.user.item.InventorySection;
import ru.homyakin.seeker.telegram.command.user.setting.PersonageSettingsCallbackUtils;

public class InlineKeyboards {
    private static final String selectedIcon = EmojiManager.getByAlias(":white_check_mark:").orElseThrow().getFirst().getEmoji();

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

    public static InlineKeyboardMarkup battlePositionKeyboard(Language language, Position currentPosition) {
        final var positions = Position.values();
        final var builder = InlineKeyboardBuilder.builder();
        for (int i = 0; i < positions.length; ++i) {
            if (i % 3 == 0) {
                builder.addRow();
            }
            final var position = positions[i];
            final String text;
            if (currentPosition == position) {
                text = selectedIcon + BattleLocalization.positionName(language, position);
            } else {
                text = BattleLocalization.positionName(language, position);
            }
            builder.addButton(
                text,
                CommandType.SELECT_BATTLE_POSITION.getText() + TextConstants.CALLBACK_DELIMITER + position.name()
            );
        }
        return builder.build();
    }

    public static InlineKeyboardMarkup joinRaidKeyboard(Language language, long chatEventId, int energyCost) {
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                RaidLocalization.joinRaidEvent(language, energyCost),
                CommandType.JOIN_RAID.getText() + TextConstants.CALLBACK_DELIMITER + chatEventId
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

    public static InlineKeyboardMarkup badgeSelector(List<AvailableBadge> badges) {
        final var builder = InlineKeyboardBuilder.builder();
        final var callbackPrefix = CommandType.SELECT_BADGE.getText() + TextConstants.CALLBACK_DELIMITER;
        for (int i = 0; i < badges.size(); ++i) {
            if (i % 4 == 0) { // по 4 элемента в строке
                builder.addRow();
            }
            final var badge = badges.get(i);
            final var callbackData = callbackPrefix + badge.badge().id().value();
            if (badge.isActive()) {
                builder.addButton(selectedIcon + badge.badge().view().icon(), callbackData);
            } else {
                builder.addButton(badge.badge().view().icon(), callbackData);
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

    public static InlineKeyboardMarkup eventIntervalsKeyboard(Language language, EventIntervals eventIntervals) {
        final var builder = InlineKeyboardBuilder.builder();
        final var intervals = eventIntervals.intervals();
        final var callbackPrefix = CommandType.TOGGLE_EVENT_INTERVAL.getText() + TextConstants.CALLBACK_DELIMITER;
        for (int i = 0; i < intervals.size(); ++i) {
            builder.addRow().addButton(GroupSettingsLocalization.eventIntervalButton(language, intervals.get(i)), callbackPrefix + i);
        }
        return builder.build();
    }

    public static InlineKeyboardMarkup leaveGroupConfirmationKeyboard(Language language, PersonageId personageId) {
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                GroupManagementLocalization.leaveGroupCancelButton(language),
                CommandType.LEAVE_GROUP_CANCEL.getText() + TextConstants.CALLBACK_DELIMITER + personageId.value()
            )
            .addButton(
                GroupManagementLocalization.leaveGroupConfirmButton(language),
                CommandType.LEAVE_GROUP_CONFIRM.getText() + TextConstants.CALLBACK_DELIMITER + personageId.value()
            )
            .build();
    }

    public static InlineKeyboardMarkup personageSettingsKeyboard(Language language, PersonageSettings settings) {
        final var builder = InlineKeyboardBuilder.builder().addRow();

        final var sendNotifications = settings.sendNotifications();
        final var autoQuesting = settings.autoQuesting();
        final var compactItems = settings.compactItems();
        builder
            .addButton(
                SettingsLocalization.sendNotificationsButton(language, sendNotifications),
                PersonageSettingsCallbackUtils.createCallback(PersonageSetting.SEND_NOTIFICATIONS, sendNotifications)
            )
            .addRow()
            .addButton(
                SettingsLocalization.autoQuestingButton(language, autoQuesting),
                PersonageSettingsCallbackUtils.createCallback(PersonageSetting.AUTO_QUESTING, autoQuesting)
            )
            .addRow()
            .addButton(
                SettingsLocalization.compactItemsButton(language, compactItems),
                PersonageSettingsCallbackUtils.createCallback(PersonageSetting.COMPACT_ITEMS, compactItems)
            );

        return builder.build();
    }

    public static InlineKeyboardMarkup joinWorldRaidKeyboard(Language language, int energy) {
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                WorldRaidLocalization.joinWorldRaidButton(language, energy),
                CommandType.JOIN_WORLD_RAID.getText()
            )
            .build();
    }

    public static InlineKeyboardMarkup welcomeUserKeyboard(Language language) {
        final var addToGroupUrl = "https://t.me/%s?startgroup".formatted(TelegramBotConfig.username());
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addUrlButton(CommonLocalization.welcomeUserAddBotToGroupButton(language), addToGroupUrl)
            .addRow()
            .addUrlButton(
                CommonLocalization.welcomeUserJoinCommunalGroupButton(language),
                TextConstants.COMMUNAL_GROUP_LINK
            )
            .addRow()
            .addButton(MenuLocalization.profileButton(language), CommandType.GET_PROFILE.getText())
            .build();
    }

    public static InlineKeyboardMarkup battleVisualizerKeyboard(Language language, String url) {
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addUrlButton(BattleLocalization.battleVisualizerButton(language), url)
            .build();
    }

    public static InlineKeyboardMarkup joinGroupConfirmationKeyboard(Language language, PersonageId personageId) {
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                GroupManagementLocalization.joinGroupCancelButton(language),
                CommandType.CANCEL_GROUP_JOIN.getText() + TextConstants.CALLBACK_DELIMITER + personageId.value()
            )
            .addButton(
                GroupManagementLocalization.joinGroupConfirmButton(language),
                CommandType.CONFIRM_GROUP_JOIN.getText() + TextConstants.CALLBACK_DELIMITER + personageId.value()
            )
            .build();
    }

    public static InlineKeyboardMarkup kickGroupMemberConfirmationKeyboard(
        Language language,
        PersonageId targetPersonageId
    ) {
        final var callbackPostfix = TextConstants.CALLBACK_DELIMITER + targetPersonageId.value();
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                GroupManagementLocalization.kickCancelButton(language),
                CommandType.CANCEL_GROUP_KICK.getText() + callbackPostfix
            )
            .addButton(
                GroupManagementLocalization.kickConfirmButton(language),
                CommandType.CONFIRM_GROUP_KICK.getText() + callbackPostfix
            )
            .build();
    }

    public static InlineKeyboardMarkup groupMembersPaginationKeyboard(Language language, int page, int totalPages) {
        if (totalPages <= 1) {
            return new InlineKeyboardMarkup(List.of());
        }
        final var callbackPrefix = CommandType.GROUP_MEMBERS_INLINE.getText() + TextConstants.CALLBACK_DELIMITER;
        final var builder = InlineKeyboardBuilder.builder().addRow();
        if (page > 1) {
            builder.addButton(
                GroupManagementLocalization.groupMembersPaginationPrevButton(language),
                callbackPrefix + (page - 1)
            );
        }
        if (page < totalPages) {
            builder.addButton(
                GroupManagementLocalization.groupMembersPaginationNextButton(language),
                callbackPrefix + (page + 1)
            );
        }
        return builder.build();
    }

    public static InlineKeyboardMarkup inventoryKeyboard(Language language) {
        final var callbackPrefix = CommandType.SELECT_INVENTORY.getText() + TextConstants.CALLBACK_DELIMITER;
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(ItemLocalization.equipmentButton(language), callbackPrefix + InventorySection.EQUIPMENT.name())
            .addButton(ItemLocalization.bagButton(language), callbackPrefix + InventorySection.BAG.name())
            .addRow()
            .addButton(ItemLocalization.loadoutsButton(language), callbackPrefix + InventorySection.LOADOUTS.name())
            .build();
    }

    public static InlineKeyboardMarkup compactInventoryKeyboard(Language language) {
        final var callbackPrefix = CommandType.SELECT_INVENTORY.getText() + TextConstants.CALLBACK_DELIMITER;
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(ItemLocalization.loadoutsButton(language), callbackPrefix + InventorySection.LOADOUTS.name())
            .build();
    }

    public static InlineKeyboardMarkup loadoutsListKeyboard(
        Language language,
        List<EquipmentLoadout> loadouts,
        boolean canCreate
    ) {
        final var builder = InlineKeyboardBuilder.builder();
        final var openPrefix = CommandType.OPEN_LOADOUT.getText() + TextConstants.CALLBACK_DELIMITER;
        for (final var loadout : loadouts) {
            builder.addRow().addButton(
                ItemLocalization.openLoadoutButton(language, loadout),
                openPrefix + loadout.id()
            );
        }
        if (canCreate) {
            builder.addRow().addButton(
                ItemLocalization.createLoadoutButton(language),
                CommandType.CREATE_LOADOUT.getText()
            );
        }
        builder.addRow()
            .addButton(
                ItemLocalization.defaultLoadoutsButton(language),
                CommandType.DEFAULT_LOADOUTS.getText()
            )
            .addButton(
                MenuLocalization.inventoryButton(language),
                CommandType.SELECT_INVENTORY.getText()
                    + TextConstants.CALLBACK_DELIMITER
                    + InventorySection.EQUIPMENT.name()
            );
        return builder.build();
    }

    public static InlineKeyboardMarkup defaultLoadoutsMenuKeyboard(Language language) {
        final var builder = InlineKeyboardBuilder.builder();
        final var openPrefix = CommandType.OPEN_DEFAULT_LOADOUT_EVENT.getText() + TextConstants.CALLBACK_DELIMITER;
        EquipmentLoadoutService.DEFAULT_LOADOUT_EVENT_TYPES.stream()
            .sorted(Comparator.comparingInt(EventType::id))
            .forEach(eventType -> builder.addRow().addButton(
                ItemLocalization.defaultLoadoutEventName(language, eventType),
                openPrefix + eventType.name()
            ));
        builder.addRow().addButton(
            ItemLocalization.backToLoadoutsButton(language),
            CommandType.LOADOUTS_LIST.getText()
        );
        return builder.build();
    }

    public static InlineKeyboardMarkup defaultLoadoutForEventKeyboard(
        Language language,
        EventType eventType,
        List<EquipmentLoadout> loadouts,
        Optional<Long> selectedLoadoutId
    ) {
        final var builder = InlineKeyboardBuilder.builder();
        final var togglePrefix = CommandType.TOGGLE_DEFAULT_LOADOUT.getText()
            + TextConstants.CALLBACK_DELIMITER
            + eventType.name()
            + TextConstants.CALLBACK_DELIMITER;
        for (final var loadout : loadouts) {
            final var isSelected = selectedLoadoutId.isPresent() && selectedLoadoutId.get() == loadout.id();
            builder.addRow().addButton(
                ItemLocalization.openLoadoutButton(language, loadout),
                togglePrefix + loadout.id(),
                isSelected ? InlineButtonStyle.SUCCESS : null
            );
        }
        builder.addRow().addButton(
            ItemLocalization.backToDefaultLoadoutsButton(language),
            CommandType.DEFAULT_LOADOUTS.getText()
        );
        return builder.build();
    }

    public static InlineKeyboardMarkup loadoutDetailKeyboard(Language language, long loadoutId) {
        final var idSuffix = TextConstants.CALLBACK_DELIMITER + loadoutId;
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                ItemLocalization.applyLoadoutButton(language),
                CommandType.APPLY_LOADOUT.getText() + idSuffix
            )
            .addButton(
                ItemLocalization.saveLoadoutButton(language),
                CommandType.SAVE_LOADOUT.getText() + idSuffix
            )
            .addRow()
            .addButton(
                ItemLocalization.renameLoadoutButton(language),
                CommandType.RENAME_LOADOUT.getText() + idSuffix
            )
            .addButton(
                ItemLocalization.deleteLoadoutButton(language),
                CommandType.DELETE_LOADOUT.getText() + idSuffix
            )
            .addRow()
            .addButton(
                ItemLocalization.backToLoadoutsButton(language),
                CommandType.LOADOUTS_LIST.getText()
            )
            .build();
    }

    public static InlineKeyboardMarkup cancelCreateLoadoutKeyboard(Language language) {
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                ItemLocalization.cancelLoadoutNameButton(language),
                CommandType.CANCEL_CREATE_LOADOUT.getText()
            )
            .build();
    }

    public static InlineKeyboardMarkup confirmDeleteLoadoutKeyboard(Language language, long loadoutId) {
        final var idSuffix = TextConstants.CALLBACK_DELIMITER + loadoutId;
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                ItemLocalization.cancelDeleteLoadoutButton(language),
                CommandType.CANCEL_DELETE_LOADOUT.getText() + idSuffix
            )
            .addButton(
                ItemLocalization.confirmDeleteLoadoutButton(language),
                CommandType.CONFIRM_DELETE_LOADOUT.getText() + idSuffix
            )
            .build();
    }

    public static InlineKeyboardMarkup confirmSellItemKeyboard(Language language, long itemId) {
        final var idSuffix = TextConstants.CALLBACK_DELIMITER + itemId;
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                ShopLocalization.cancelSellButton(language),
                CommandType.CANCEL_SELL_ITEM.getText() + idSuffix
            )
            .addButton(
                ShopLocalization.confirmSellButton(language),
                CommandType.CONFIRM_SELL_ITEM.getText() + idSuffix
            )
            .build();
    }

    public static InlineKeyboardMarkup confirmOutpostDonateKeyboard(
        Language language,
        Building building,
        long itemId
    ) {
        final var idSuffix = TextConstants.CALLBACK_DELIMITER + building.id()
            + TextConstants.CALLBACK_DELIMITER + itemId;
        return InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                OutpostLocalization.cancelDonateButton(language),
                CommandType.CANCEL_OUTPOST_DONATE.getText() + idSuffix
            )
            .addButton(
                OutpostLocalization.confirmDonateButton(language),
                CommandType.CONFIRM_OUTPOST_DONATE.getText() + idSuffix
            )
            .build();
    }

    public static InlineKeyboardMarkup emptyKeyboard() {
        return new InlineKeyboardMarkup(List.of());
    }
}
