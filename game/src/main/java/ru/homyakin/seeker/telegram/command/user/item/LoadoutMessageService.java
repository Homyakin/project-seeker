package ru.homyakin.seeker.telegram.command.user.item;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.game.item.loadout.entity.EquipmentLoadout;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.personage.settings.action.GetPersonageSettingsCommand;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;

@Component
public class LoadoutMessageService {
    private final EquipmentLoadoutService loadoutService;
    private final ItemService itemService;
    private final GetPersonageSettingsCommand getPersonageSettingsCommand;
    private final TelegramSender telegramSender;

    public LoadoutMessageService(
        EquipmentLoadoutService loadoutService,
        ItemService itemService,
        GetPersonageSettingsCommand getPersonageSettingsCommand,
        TelegramSender telegramSender
    ) {
        this.loadoutService = loadoutService;
        this.itemService = itemService;
        this.getPersonageSettingsCommand = getPersonageSettingsCommand;
        this.telegramSender = telegramSender;
    }

    public void editLoadoutsList(User user, int messageId) {
        final var loadouts = loadoutService.list(user.personageId());
        final var inventory = itemService.getPersonageItems(user.personageId());
        final var ownedById = inventory.items().stream()
            .collect(Collectors.toMap(PersonageItem::id, item -> item));
        final var battleStatsByLoadoutId = new HashMap<Long, BattlePersonage>();
        for (final var loadout : loadouts) {
            final var wornItems = loadout.itemIds().stream()
                .map(ownedById::get)
                .filter(Objects::nonNull)
                .toList();
            final var combatItems = itemService.itemsWithDefaults(wornItems);
            battleStatsByLoadoutId.put(
                loadout.id(),
                new BattlePersonage(combatItems, loadout.battlePosition())
            );
        }
        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(messageId)
                .text(ItemLocalization.loadoutsList(user.language(), loadouts, battleStatsByLoadoutId))
                .keyboard(InlineKeyboards.loadoutsListKeyboard(
                    user.language(),
                    loadouts,
                    loadoutService.canCreate(user.personageId())
                ))
                .build()
        );
    }

    public void editLoadoutDetail(User user, int messageId, EquipmentLoadout loadout) {
        final var inventory = itemService.getPersonageItems(user.personageId());
        final var compactItems = getPersonageSettingsCommand.execute(user.personageId()).compactItems();
        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(messageId)
                .text(ItemLocalization.loadoutDetail(user.language(), loadout, inventory, compactItems))
                .keyboard(InlineKeyboards.loadoutDetailKeyboard(user.language(), loadout.id()))
                .build()
        );
    }

    public void editCreateLoadoutPrompt(User user, int messageId) {
        final var inventory = itemService.getPersonageItems(user.personageId());
        final var compactItems = getPersonageSettingsCommand.execute(user.personageId()).compactItems();
        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(messageId)
                .text(ItemLocalization.initCreateLoadout(user.language(), inventory, compactItems))
                .keyboard(InlineKeyboards.cancelCreateLoadoutKeyboard(user.language()))
                .build()
        );
    }

    public void editDefaultLoadoutsMenu(User user, int messageId) {
        final var loadoutsById = loadoutService.list(user.personageId()).stream()
            .collect(Collectors.toMap(EquipmentLoadout::id, loadout -> loadout));
        final var defaultsByEventType = loadoutService.getDefaults(user.personageId()).entrySet().stream()
            .filter(entry -> loadoutsById.containsKey(entry.getValue()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> loadoutsById.get(entry.getValue())
            ));
        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(messageId)
                .text(ItemLocalization.defaultLoadoutsMenu(user.language(), defaultsByEventType))
                .keyboard(InlineKeyboards.defaultLoadoutsMenuKeyboard(user.language()))
                .build()
        );
    }

    public void editDefaultLoadoutForEvent(User user, int messageId, EventType eventType) {
        final var loadouts = loadoutService.list(user.personageId());
        final var selectedLoadoutId = Optional.ofNullable(loadoutService.getDefaults(user.personageId()).get(eventType));
        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(messageId)
                .text(ItemLocalization.defaultLoadoutForEvent(user.language(), eventType))
                .keyboard(InlineKeyboards.defaultLoadoutForEventKeyboard(
                    user.language(),
                    eventType,
                    loadouts,
                    selectedLoadoutId
                ))
                .build()
        );
    }
}
