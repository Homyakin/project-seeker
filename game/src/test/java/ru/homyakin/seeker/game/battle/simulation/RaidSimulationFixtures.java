package ru.homyakin.seeker.game.battle.simulation;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.item.catalog.ItemObjectsToml;
import ru.homyakin.seeker.game.item.models.DefaultItems;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.utils.ResourceUtils;

public final class RaidSimulationFixtures {
    private static final String TOML_PATH = "game-data/item_objects_catalog.toml";
    private static final Map<String, ItemObject> OBJECTS_BY_CODE = ResourceUtils.calc(TOML_PATH, ItemObjectsToml::load)
        .orElseThrow(() -> new IllegalStateException("Missing " + TOML_PATH))
        .itemObjects()
        .stream()
        .collect(Collectors.toUnmodifiableMap(ItemObject::code, Function.identity()));
    private static final List<Item> DEFAULT_ITEMS = List.of(
        DefaultItems.MAIN_FIST,
        DefaultItems.OFF_FIST,
        DefaultItems.SHIRT,
        DefaultItems.PANTS,
        DefaultItems.SHOES,
        DefaultItems.HELMET,
        DefaultItems.GLOVES
    );

    private RaidSimulationFixtures() {
    }

    public static List<ReferenceBuild> parseComposition(String value, int partySize) {
        if (partySize <= 0) {
            throw new IllegalArgumentException("partySize must be positive");
        }
        final var parsed = Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(it -> !it.isEmpty())
            .map(it -> it.toUpperCase(Locale.ROOT))
            .map(ReferenceBuild::valueOf)
            .toList();
        if (parsed.size() == 1) {
            return IntStream.range(0, partySize).mapToObj(ignored -> parsed.getFirst()).toList();
        }
        if (parsed.size() != partySize) {
            throw new IllegalArgumentException(
                "Composition must contain either one build or exactly %d builds".formatted(partySize)
            );
        }
        return parsed;
    }

    public static List<BattlePersonage> team(List<ReferenceBuild> composition) {
        return composition.stream().map(RaidSimulationFixtures::personage).toList();
    }

    public static String loadoutLabel(List<ReferenceBuild> composition) {
        final var distinct = composition.stream().distinct().toList();
        return distinct.size() == 1 ? distinct.getFirst().name() : "MIXED";
    }

    public static String compositionLabel(List<ReferenceBuild> composition) {
        return composition.stream().map(ReferenceBuild::name).collect(Collectors.joining("+"));
    }

    private static BattlePersonage personage(ReferenceBuild build) {
        final List<Item> items = build == ReferenceBuild.VIRTUAL_DEFAULT
            ? DEFAULT_ITEMS
            : build.itemCodes().stream().map(RaidSimulationFixtures::item).toList();
        return new BattlePersonage(items, build.position(), Optional.of(build.name()));
    }

    private static Item item(String code) {
        final var object = OBJECTS_BY_CODE.get(code);
        if (object == null) {
            throw new IllegalStateException("Unknown item object in simulation fixture: " + code);
        }
        return Item.fromObject(object);
    }

    public enum ReferenceBuild {
        VIRTUAL_DEFAULT(Position.FRONT, List.of()),
        PLATE_TANK(
            Position.FRONT,
            List.of("mace", "tower_shield", "cuirass", "greaves", "sabatons", "great_helm", "gauntlets")
        ),
        LEATHER_PIERCE(
            Position.FRONT,
            List.of("spear", "dirk", "breastplate", "leather_chausses", "boots", "leather_helm", "leather_gloves")
        ),
        LEATHER_SLASH(
            Position.FRONT,
            List.of("sword", "shortsword", "breastplate", "leather_chausses", "boots", "leather_helm", "leather_gloves")
        ),
        ARCANE_MAGE(
            Position.BACK,
            List.of("staff", "orb", "wizard_robe", "arcane_chausses", "arcane_boots", "circlet", "arcane_gloves")
        ),
        CLOTH_MAGE(
            Position.BACK,
            List.of("staff", "orb", "robe", "cloth_chausses", "cloth_boots", "hood", "cloth_gloves")
        ),
        ;

        private final Position position;
        private final List<String> itemCodes;

        ReferenceBuild(Position position, List<String> itemCodes) {
            this.position = position;
            this.itemCodes = itemCodes;
        }

        public Position position() {
            return position;
        }

        public List<String> itemCodes() {
            return itemCodes;
        }
    }
}
