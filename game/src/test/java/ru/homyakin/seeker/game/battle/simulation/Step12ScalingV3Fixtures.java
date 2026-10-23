package ru.homyakin.seeker.game.battle.simulation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.Modifier;

/**
 * Rebuilds the frozen step 12 compositions with objects from the third scaling catalog.
 * The composition, modifier carrier, rarity, order and enhance level remain those of the
 * frozen {@link Step12SimulationFixtures} definitions.
 */
public final class Step12ScalingV3Fixtures {
    private static final Map<String, ItemObject> OBJECTS_BY_CODE;
    private static final Map<String, Modifier> MODIFIERS_BY_CODE;

    static {
        final var catalog = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V3);
        OBJECTS_BY_CODE = catalog.itemObjects().stream().collect(Collectors.toUnmodifiableMap(
            ItemObject::code,
            Function.identity()
        ));
        MODIFIERS_BY_CODE = catalog.modifiers().stream().collect(Collectors.toUnmodifiableMap(
            Modifier::code,
            Function.identity()
        ));
    }

    private Step12ScalingV3Fixtures() {
    }

    public static List<Item> items(Step12SimulationFixtures.RoleBuild build, int enhanceLevel) {
        return remap(Step12SimulationFixtures.items(build, enhanceLevel));
    }

    public static List<Item> items(Step12SimulationFixtures.MixedBuild build, int enhanceLevel) {
        return remap(Step12SimulationFixtures.items(build, enhanceLevel));
    }

    public static BattlePersonage personage(
        Step12SimulationFixtures.RoleBuild build,
        int enhanceLevel
    ) {
        return personage(
            items(build, enhanceLevel),
            build.position(),
            build.targetingTactic(),
            build.displayName()
        );
    }

    public static BattlePersonage personage(
        Step12SimulationFixtures.MixedBuild build,
        int enhanceLevel
    ) {
        return personage(
            items(build, enhanceLevel),
            build.position(),
            build.targetingTactic(),
            build.displayName()
        );
    }

    private static List<Item> remap(List<Item> source) {
        return source.stream().map(item -> new Item(
            requireObject(item.object().code()),
            item.modifier().map(modifier -> requireModifier(modifier.code())),
            item.rarity(),
            item.enhanceLevel()
        )).toList();
    }

    private static ItemObject requireObject(String code) {
        final var object = OBJECTS_BY_CODE.get(code);
        if (object == null) {
            throw new IllegalStateException("SCALING_V3 has no item object used by step 12: " + code);
        }
        return object;
    }

    private static Modifier requireModifier(String code) {
        final var modifier = MODIFIERS_BY_CODE.get(code);
        if (modifier == null) {
            throw new IllegalStateException("SCALING_V3 has no modifier used by step 12: " + code);
        }
        return modifier;
    }

    private static BattlePersonage personage(
        List<Item> items,
        Position position,
        TargetingTactic tactic,
        String name
    ) {
        final var personage = BattlePersonage.forScalingSkillsV2Combat(
            items,
            position,
            Map.of(),
            Optional.of(name)
        );
        personage.setTargetingTactic(tactic);
        return personage;
    }
}
