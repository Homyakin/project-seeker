package ru.homyakin.seeker.game.item;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.database.ItemCatalogLifecycleDao;
import ru.homyakin.seeker.game.item.database.ItemModifierDao;
import ru.homyakin.seeker.game.item.database.ItemObjectDao;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.Modifier;

@Service
public class ItemCatalogService {
    private final ItemObjectDao itemObjectDao;
    private final ItemModifierDao itemModifierDao;
    private final ItemCatalogLifecycleDao itemCatalogLifecycleDao;

    public ItemCatalogService(
        ItemObjectDao itemObjectDao,
        ItemModifierDao itemModifierDao,
        ItemCatalogLifecycleDao itemCatalogLifecycleDao
    ) {
        this.itemObjectDao = itemObjectDao;
        this.itemModifierDao = itemModifierDao;
        this.itemCatalogLifecycleDao = itemCatalogLifecycleDao;
    }

    @Transactional
    public void stageRelease(
        EquipmentCatalogVersion version,
        List<ItemObject> previousItemObjects,
        List<Modifier> previousModifiers
    ) {
        final var catalog = EquipmentCatalogLoader.loadValidated(version);
        final var initiallyAvailableItemCodes = previousItemObjects.stream()
            .map(ItemObject::code)
            .collect(Collectors.toUnmodifiableSet());
        final var initiallyAvailableModifierCodes = previousModifiers.stream()
            .map(Modifier::code)
            .collect(Collectors.toUnmodifiableSet());
        requireUniqueCodes(previousItemObjects.size(), initiallyAvailableItemCodes, "item object");
        requireUniqueCodes(previousModifiers.size(), initiallyAvailableModifierCodes, "modifier");
        final var releaseItemCodes = catalog.itemObjects().stream()
            .map(item -> item.code())
            .collect(Collectors.toUnmodifiableSet());
        final var releaseModifierCodes = catalog.modifiers().stream()
            .map(modifier -> modifier.code())
            .collect(Collectors.toUnmodifiableSet());
        requireSubset(initiallyAvailableItemCodes, releaseItemCodes, "item object");
        requireSubset(initiallyAvailableModifierCodes, releaseModifierCodes, "modifier");

        final boolean firstStage = itemCatalogLifecycleDao.registerStagedIfMissing(version);
        if (firstStage) {
            prepareRollbackBaseline(previousItemObjects, previousModifiers);
        }
        final boolean releaseActive = itemCatalogLifecycleDao.isActive(version);

        catalog.itemObjects().forEach(item -> itemObjectDao.save(
            item,
            releaseActive || initiallyAvailableItemCodes.contains(item.code())
        ));
        catalog.modifiers().forEach(modifier -> itemModifierDao.save(
            modifier,
            releaseActive || initiallyAvailableModifierCodes.contains(modifier.code())
        ));
        if (firstStage) {
            itemObjectDao.setAcquisitionEnabledOnly(initiallyAvailableItemCodes);
            itemModifierDao.setAssignmentEnabledOnly(initiallyAvailableModifierCodes);
        }
    }

    @Transactional
    void activateRelease(EquipmentCatalogVersion version) {
        final var catalog = EquipmentCatalogLoader.loadValidated(version);
        if (!itemCatalogLifecycleDao.activateIfStaged(version)) {
            return;
        }
        itemObjectDao.setAcquisitionEnabledOnly(catalog.itemObjects().stream()
            .map(item -> item.code())
            .collect(Collectors.toUnmodifiableSet()));
        itemModifierDao.setAssignmentEnabledOnly(catalog.modifiers().stream()
            .map(modifier -> modifier.code())
            .collect(Collectors.toUnmodifiableSet()));
    }

    private void prepareRollbackBaseline(List<ItemObject> itemObjects, List<Modifier> modifiers) {
        final var backupState = itemCatalogLifecycleDao.getCatalogBackupState();
        if (backupState.hasSnapshot()) {
            return;
        }
        if (!backupState.isFreshEmptyCatalog()) {
            throw new IllegalStateException("Catalog rollback baseline is partial or inconsistent: " + backupState);
        }

        itemObjects.forEach(item -> itemObjectDao.save(item, true));
        modifiers.forEach(modifier -> itemModifierDao.save(modifier, true));
        final int savedItemObjects = itemCatalogLifecycleDao.snapshotItemObjects();
        final int savedModifiers = itemCatalogLifecycleDao.snapshotItemModifiers();
        if (savedItemObjects != itemObjects.size() || savedModifiers != modifiers.size()) {
            throw new IllegalStateException(
                "Unexpected catalog rollback baseline size: item objects %d/%d, modifiers %d/%d"
                    .formatted(savedItemObjects, itemObjects.size(), savedModifiers, modifiers.size())
            );
        }
    }

    private static void requireUniqueCodes(int count, Set<String> codes, String entityName) {
        if (count != codes.size()) {
            throw new IllegalArgumentException("Previous " + entityName + " codes must be unique");
        }
    }

    private static void requireSubset(Set<String> initial, Set<String> release, String entityName) {
        if (initial.isEmpty()) {
            throw new IllegalArgumentException("Initial " + entityName + " codes must not be empty");
        }
        if (!release.containsAll(initial)) {
            final var missing = initial.stream().filter(code -> !release.contains(code)).sorted().toList();
            throw new IllegalArgumentException("Initial " + entityName + " codes missing from release: " + missing);
        }
    }
}
