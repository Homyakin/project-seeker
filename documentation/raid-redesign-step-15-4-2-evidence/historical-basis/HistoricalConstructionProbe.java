import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import ru.homyakin.seeker.game.battle.Battle;
import ru.homyakin.seeker.game.battle.BattleContext;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.simulation.RaidSimulationFixtures;
import ru.homyakin.seeker.game.battle.simulation.RaidSimulationFixtures.ReferenceBuild;
import ru.homyakin.seeker.game.event.raid.generator.MyconidColonyGenerator;
import ru.homyakin.seeker.game.item.catalog.ItemModifiersToml;
import ru.homyakin.seeker.game.item.catalog.ItemObjectsToml;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.ResourceUtils;

/** Construction only. Deliberately contains no call to Battle.process. */
public final class HistoricalConstructionProbe {
    private static final String COMMIT = "6150d562546cca22bed139c4d0fa23456e4d801e";
    private static final List<ReferenceBuild> T3 = List.of(
        ReferenceBuild.LEATHER_PIERCE, ReferenceBuild.ARCANE_MAGE, ReferenceBuild.PLATE_TANK
    );

    public static void main(String[] args) throws Exception {
        final var mapper = new ObjectMapper().registerModule(new Jdk8Module());
        final var objects = ResourceUtils.calc("game-data/item_objects_catalog.toml", ItemObjectsToml::load)
            .orElseThrow().itemObjects();
        final var mods = ResourceUtils.calc("game-data/item_modifiers_catalog.toml", ItemModifiersToml::load)
            .orElseThrow().modifiers();
        final var currentPath = Path.of("/home/user/projects/project-seeker/game/src/main/resources/game-data/catalog/scaling_v5/item_objects.toml");
        final var historicalCodes = new TreeSet<String>();
        final var currentCodes = new TreeSet<String>();
        objects.forEach(o -> historicalCodes.add(o.code()));
        final var pattern = java.util.regex.Pattern.compile("^code\\s*=\\s*\"([^\"]+)\"", java.util.regex.Pattern.MULTILINE);
        final var matches = pattern.matcher(Files.readString(currentPath));
        while (matches.find()) currentCodes.add(matches.group(1));
        final var common = new TreeSet<>(historicalCodes);
        common.retainAll(currentCodes);
        final var added = new TreeSet<>(currentCodes);
        added.removeAll(historicalCodes);
        final var missing = new TreeSet<>(historicalCodes);
        missing.removeAll(currentCodes);
        final var result = new LinkedHashMap<String,Object>();
        result.put("purpose", "HISTORICAL_CONSTRUCTION_ONLY_NO_BATTLES");
        result.put("sourceCommit", COMMIT);
        result.put("battleClassLocation", Battle.class.getProtectionDomain().getCodeSource().getLocation().toString());
        result.put("personageClassLocation", BattlePersonage.class.getProtectionDomain().getCodeSource().getLocation().toString());
        result.put("oldCatalogCount", objects.size());
        result.put("oldModifiers", mods);
        result.put("commonItemCodes", common);
        result.put("newOnlyItemCodes", added);
        result.put("oldOnlyItemCodes", missing);
        result.put("oldCatalog", objects);
        result.put("fixtureT3DurationReference", T3);
        result.put("construction", RandomUtils.withSeed(20260901L, () -> {
            final var cases = new ArrayList<Object>();
            cases.add(construct("T3_MIXED_COMMON", T3, 10));
            final var t4 = new ArrayList<>(T3);
            t4.add(ReferenceBuild.LEATHER_PIERCE);
            cases.add(construct("T4_MIXED_PLUS_ORDINARY_COMMON", t4, 10));
            cases.add(construct("S1_ARCANE_COMMON", List.of(ReferenceBuild.ARCANE_MAGE), 10));
            cases.add(construct("S1_ARCANE_PLUS_VIRTUAL_DEFAULT", List.of(ReferenceBuild.ARCANE_MAGE, ReferenceBuild.VIRTUAL_DEFAULT), 10));
            return cases;
        }));
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
    }

    private static Object construct(String name, List<ReferenceBuild> composition, int level) {
        final var players = RaidSimulationFixtures.team(composition);
        final double q = 1 + ((level - 10) * .06 + .14);
        final var enemies = new MyconidColonyGenerator().generate(players, q);
        final int count = Math.max(2, players.size());
        final double g = 1 + .05 * MathUtils.log(count <= 7 ? 2.2 : 4.0, count);
        final var output = new LinkedHashMap<String,Object>();
        output.put("name", name);
        output.put("level", level);
        output.put("q", q);
        output.put("g", g);
        output.put("targetPower", players.stream().mapToDouble(BattlePersonage::power).sum() * q * g * .77);
        output.put("actualEnemyPower", enemies.stream().mapToDouble(BattlePersonage::power).sum());
        output.put("playersBeforeContext", players.stream().map(HistoricalConstructionProbe::snapshot).toList());
        output.put("enemiesBeforeContext", enemies.stream().map(HistoricalConstructionProbe::snapshot).toList());
        new BattleContext(players, enemies);
        output.put("playersAfterContext", players.stream().map(HistoricalConstructionProbe::snapshot).toList());
        output.put("enemiesAfterContext", enemies.stream().map(HistoricalConstructionProbe::snapshot).toList());
        return output;
    }

    private static Object snapshot(BattlePersonage p) {
        final var s = new LinkedHashMap<String,Object>();
        s.put("id", p.id()); s.put("name", p.name()); s.put("hp", p.maxHealth());
        s.put("power", p.power()); s.put("range", p.range()); s.put("positionDeclared", p.startPosition());
        s.put("lineIndex", p.currentPosition()); s.put("speed", p.initiative()); s.put("gauge", p.initiativeGauge());
        s.put("crit", p.critChance()); s.put("dodge", p.dodgeChance()); s.put("critMultiplier", p.critMultiplier());
        s.put("defense", p.defenses()); s.put("attackByRange", p.attacksByRange());
        s.put("defenseMultipliers", p.defenseReductions()); s.put("skills", p.skillSnapshots());
        s.put("items", p.itemSnapshots());
        return s;
    }
}
