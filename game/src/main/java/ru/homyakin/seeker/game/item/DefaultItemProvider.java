package ru.homyakin.seeker.game.item;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.catalog.EquipmentReleaseConfig;
import ru.homyakin.seeker.game.item.database.ItemCatalogLifecycleDao;
import ru.homyakin.seeker.game.item.models.DefaultItems;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

/** Supplies free-slot equipment only after the selected release has actually been activated. */
@Component
public final class DefaultItemProvider {
    private static final Map<PersonageSlot, Item> LEGACY_ITEMS = Arrays.stream(PersonageSlot.values())
        .collect(Collectors.toUnmodifiableMap(
            Function.identity(),
            slot -> DefaultItems.defaultItemForSlot(slot, Set.of()).orElseThrow()
        ));
    private static final Map<EquipmentCatalogVersion, Map<PersonageSlot, Item>> RELEASE_DEFAULTS = new ConcurrentHashMap<>();
    private final Supplier<Map<PersonageSlot, Item>> selectedItems;

    @Autowired
    public DefaultItemProvider(EquipmentReleaseConfig config, ItemCatalogLifecycleDao lifecycleDao) {
        selectedItems = () -> {
            final var version = config.catalogVersion();
            if ((version == EquipmentCatalogVersion.SCALING_V4 || version == EquipmentCatalogVersion.SCALING_V5)
                && lifecycleDao.getActiveVersions().contains(version)) {
                return RELEASE_DEFAULTS.computeIfAbsent(version, DefaultItemProvider::loadDefaults);
            }
            return LEGACY_ITEMS;
        };
    }

    private DefaultItemProvider(Supplier<Map<PersonageSlot, Item>> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public static DefaultItemProvider legacy() {
        return new DefaultItemProvider(() -> LEGACY_ITEMS);
    }

    /** One immutable snapshot can be reused throughout a batch of personages. */
    public Map<PersonageSlot, Item> defaultsBySlot() {
        return selectedItems.get();
    }

    private static Map<PersonageSlot, Item> loadDefaults(EquipmentCatalogVersion version) {
        return EquipmentCatalogLoader.loadValidated(version).defaultItems().stream()
            .collect(Collectors.toUnmodifiableMap(
                object -> object.slots().iterator().next(),
                object -> new Item(object, Optional.empty(), ItemRarity.COMMON, 0)
            ));
    }
}
