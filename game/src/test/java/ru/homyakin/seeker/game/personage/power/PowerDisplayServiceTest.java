package ru.homyakin.seeker.game.personage.power;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.BattlePersonageStats;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

class PowerDisplayServiceTest {
    private static final JsonMapper MAPPER = JsonMapper.builder().addModule(new Jdk8Module()).build();

    @Test
    void fixedReferenceCoversEverySlotAndDisplaysExactlyOneThousand() {
        final var items = referenceItems();
        final var slots = items.stream().flatMap(item -> item.object().slots().stream()).toList();
        final var power = ReferencePowerCalculator.calculate(items, PowerDisplayVersion.REFERENCE_V1_SCALE);

        Assertions.assertAll(
            () -> Assertions.assertEquals(7, slots.size()),
            () -> Assertions.assertEquals(Set.of(PersonageSlot.values()), Set.copyOf(slots)),
            () -> Assertions.assertEquals(2_280, items.stream().mapToInt(Item::health).sum()),
            () -> Assertions.assertEquals(140, items.stream().mapToInt(Item::speed).sum()),
            () -> Assertions.assertEquals(301_606.92127471056, power.unscaled(), 1e-7),
            () -> Assertions.assertEquals(1_000, power.displayed(), 1e-9),
            () -> Assertions.assertEquals(1_000, PowerDisplayVersion.REFERENCE_V1.referenceValue(items))
        );
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void proposedCandidateDoesNotChangeTheFrozenReferenceScale(EquipmentCatalogVersion version) {
        final var objects = EquipmentCatalogLoader.loadValidated(version).itemObjects()
            .stream().collect(Collectors.toMap(ItemObject::code, item -> item));
        final var items = PowerDisplayVersion.REFERENCE_V1_ITEM_CODES.stream()
            .map(code -> new Item(objects.get(code), Optional.empty(), ItemRarity.COMMON, 0))
            .toList();
        Assertions.assertEquals(1_000, PowerDisplayVersion.REFERENCE_V1.referenceValue(items));
    }

    @Test
    void legacyGateRetainsPreviousDisplayAndReferenceGateUsesEquipmentOnly() {
        final var items = referenceItems();
        final var personage = new BattlePersonage(items, Position.FRONT);
        final var legacy = new PowerDisplayService(PowerDisplayVersion.LEGACY_V1);
        final var reference = new PowerDisplayService(PowerDisplayVersion.REFERENCE_V1);

        Assertions.assertAll(
            () -> Assertions.assertEquals(((int) personage.legacyPower()) / 100, legacy.value(items, personage)),
            () -> Assertions.assertEquals(1_000, reference.value(items, personage)),
            () -> Assertions.assertEquals(1_000, reference.value(items)),
            () -> Assertions.assertEquals(0, reference.value(List.of()))
        );
    }

    @Test
    void roundingIsNearestIntegerAndRejectsOverflow() {
        final var item = synthetic(Integer.MAX_VALUE, Integer.MAX_VALUE);
        Assertions.assertThrows(ArithmeticException.class, () -> PowerDisplayVersion.REFERENCE_V1.referenceValue(List.of(item)));
        Assertions.assertThrows(IllegalStateException.class, () -> PowerDisplayVersion.LEGACY_V1.referenceValue(referenceItems()));
        final var ordinary = synthetic(100, 100);
        final var raw = ReferencePowerCalculator.calculate(List.of(ordinary), PowerDisplayVersion.REFERENCE_V1_SCALE);
        Assertions.assertEquals(Math.round(raw.displayed()), PowerDisplayVersion.REFERENCE_V1.referenceValue(List.of(ordinary)));
    }

    @Test
    void snapshotIsOptionalForLegacyOrUnrepresentableEquipmentAndNeverChangesCombatAcceptance() {
        Assertions.assertTrue(PowerDisplaySnapshot.forEquipment(List.of()).isEmpty());
        Assertions.assertTrue(PowerDisplaySnapshot.forEquipment(List.of(Item.stats(0, 0, 0, 100, 0))).isEmpty());
        Assertions.assertTrue(PowerDisplaySnapshot.forEquipment(List.of(synthetic(Integer.MAX_VALUE, Integer.MAX_VALUE))).isEmpty());
        Assertions.assertEquals(1_000, PowerDisplaySnapshot.forEquipment(referenceItems()).orElseThrow().value());
    }

    @Test
    void reportUsesOnlySavedSnapshotAndOnlyUnderExplicitReferenceGate() {
        final var snapshot = Optional.of(PowerDisplaySnapshot.referenceV1(referenceItems()));
        final var legacy = new PowerDisplayService(PowerDisplayVersion.LEGACY_V1);
        final var reference = new PowerDisplayService(PowerDisplayVersion.REFERENCE_V1);
        Assertions.assertAll(
            () -> Assertions.assertEquals(Optional.empty(), legacy.battleReportValue(snapshot)),
            () -> Assertions.assertEquals(Optional.empty(), reference.battleReportValue(Optional.empty())),
            () -> Assertions.assertEquals(Optional.of("1000"), reference.battleReportValue(snapshot))
        );
    }

    @Test
    void oldStatsJsonWithoutSnapshotStillReadsAndNewSnapshotRoundTrips() throws Exception {
        final var oldStats = stats(Optional.empty());
        final var oldTree = MAPPER.valueToTree(oldStats);
        Assertions.assertFalse(oldTree.has("displayedPower"));
        ((com.fasterxml.jackson.databind.node.ObjectNode) oldTree).remove("displayedPower");
        Assertions.assertEquals(oldStats, MAPPER.treeToValue(oldTree, BattlePersonageStats.class));
        final var newStats = stats(Optional.of(PowerDisplaySnapshot.referenceV1(referenceItems())));
        Assertions.assertEquals(newStats, MAPPER.readValue(MAPPER.writeValueAsString(newStats), BattlePersonageStats.class));
        Assertions.assertEquals(oldStats.damageDealt(), newStats.damageDealt());
        Assertions.assertEquals(oldStats.damageTaken(), newStats.damageTaken());
    }

    static List<Item> referenceItems() {
        final var objects = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V3).itemObjects()
            .stream().collect(Collectors.toMap(ItemObject::code, item -> item));
        return PowerDisplayVersion.REFERENCE_V1_ITEM_CODES.stream()
            .map(code -> new Item(objects.get(code), Optional.empty(), ItemRarity.COMMON, 0))
            .toList();
    }

    private static Item synthetic(int health, int attack) {
        return new Item(new ItemObject(
            "synthetic", Set.of(PersonageSlot.MAIN_HAND),
            List.of(new ru.homyakin.seeker.game.item.models.ItemAttack(
                ru.homyakin.seeker.game.item.models.AttackType.SLASH, 1, 1, attack
            )), Optional.empty(), health, 0, 0, 0, 1_000, 0, 0, ItemProgressionVersion.V1, Map.of()
        ), Optional.empty(), ItemRarity.COMMON, 0);
    }

    private static BattlePersonageStats stats(Optional<PowerDisplaySnapshot> snapshot) {
        return new BattlePersonageStats(100, 50, 20, 10, 100, 3, 50, 1, 10, 1, 20, 1, 0, 4, 25, 1, snapshot);
    }
}
