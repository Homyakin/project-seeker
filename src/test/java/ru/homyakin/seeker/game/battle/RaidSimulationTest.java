package ru.homyakin.seeker.game.battle;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.event.launched.RaidParams;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.raid.generator.RaidGenerator;
import ru.homyakin.seeker.game.event.raid.models.LaunchedRaidEvent;
import ru.homyakin.seeker.game.event.raid.models.RaidType;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Win-rate simulation comparing Common vs Legendary gear for a fixed Tank+DD duo
 * against all raid types at levels 5, 10 and 15.
 *
 * <p>Marked {@code @Disabled} because it runs 24 000 battles (may take ~30 s).
 * Remove the annotation to execute.</p>
 */
@Disabled("long-running raid simulation — 24 000 battles")
class RaidSimulationTest {

    private static final int ITERATIONS = 1000;
    private static final List<Integer> RAID_LEVELS = List.of(5, 10, 15);
    private static final List<RaidType> RAID_TYPES = List.of(
        RaidType.WOLFPACK, RaidType.ZOMBIE_HORDE,
        RaidType.MYCONID_COLONY, RaidType.MAGGEESE_FLOCK
    );

    private final RaidGenerator raidGenerator = new RaidGenerator();
    private final Battle battle = new Battle();

    // ------------------------------------------------------------------ stats

    private record SimStats(
        int wins,
        long totalRounds,
        long tankDamageDealt, long ddDamageDealt,
        long tankDamageTaken, long ddDamageTaken,
        long tankHpLost, long ddHpLost,
        int tankSurvived, int ddSurvived
    ) {
        static SimStats zero() {
            return new SimStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        SimStats accumulate(SimStats o) {
            return new SimStats(
                wins + o.wins, totalRounds + o.totalRounds,
                tankDamageDealt + o.tankDamageDealt, ddDamageDealt + o.ddDamageDealt,
                tankDamageTaken + o.tankDamageTaken, ddDamageTaken + o.ddDamageTaken,
                tankHpLost + o.tankHpLost, ddHpLost + o.ddHpLost,
                tankSurvived + o.tankSurvived, ddSurvived + o.ddSurvived
            );
        }

        double winRate() { return wins * 100.0 / ITERATIONS; }
        double avgRounds() { return (double) totalRounds / ITERATIONS; }
        double avgTankDmgDealt() { return (double) tankDamageDealt / ITERATIONS; }
        double avgDdDmgDealt() { return (double) ddDamageDealt / ITERATIONS; }
        double avgTankDmgTaken() { return (double) tankDamageTaken / ITERATIONS; }
        double avgDdDmgTaken() { return (double) ddDamageTaken / ITERATIONS; }
        double avgTankHpLost() { return (double) tankHpLost / ITERATIONS; }
        double avgDdHpLost() { return (double) ddHpLost / ITERATIONS; }
        double tankSurvive() { return tankSurvived * 100.0 / ITERATIONS; }
        double ddSurvive() { return ddSurvived * 100.0 / ITERATIONS; }
    }

    // -------------------------------------------------------------- main test

    @Test
    void compareCommonVsLegendaryTankDdDuo() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("  RAID SIMULATION: Common vs Legendary");
        System.out.println("  Tank (Plate/Mace) + DD (Arcane/Crossbow)");
        System.out.println("  Iterations: " + ITERATIONS);
        System.out.println("========================================");

        for (final var raidType : RAID_TYPES) {
            System.out.printf("%n=== %s ===%n", raidType);
            for (final boolean legendary : List.of(false, true)) {
                System.out.printf("%n-- %s (Tank+DD) --%n", legendary ? "Legendary" : "Common");
                printGearStats(legendary);

                final var results = new SimStats[RAID_LEVELS.size()];
                for (int i = 0; i < RAID_LEVELS.size(); i++) {
                    results[i] = runSimulation(raidType, RAID_LEVELS.get(i), legendary);
                }

                printTable(results);
            }
        }
    }

    // -------------------------------------------------------- item factories

    private static Item item(
        String code, Set<PersonageSlot> slots,
        Optional<ItemAttack> attack, Optional<ItemDefense> defense,
        int health, int critChance, int dodgeChance,
        double critMultiplier, int speed, int baseThreat,
        Optional<ActiveEnum> modifier
    ) {
        final var obj = new ItemObject(code, slots, attack, defense,
            health, critChance, dodgeChance, critMultiplier,
            speed, baseThreat, Map.of());
        return modifier
            .map(e -> new Item(obj, Optional.of(new Modifier(e)), ItemRarity.LEGENDARY))
            .orElseGet(() -> new Item(obj, Optional.empty(), ItemRarity.COMMON));
    }

    // --------------------------------------------------------- gear builders

    /**
     * Tank: full PLATE set, mace (BLUNT, range 1) + tower shield.
     * Legendary modifiers:
     *   Mace → BERSERK, Shield → KNOCKBACK,
     *   Cuirass → THORNS, Greaves → SELF_HEAL,
     *   Sabatons → SELF_HEAL (stack → rank V),
     *   Great Helm → COUNTER_ATTACK,
     *   Gauntlets → THORNS (stack → rank V)
     */
    private static List<Item> tankGear(boolean legendary) {
        final Optional<ActiveEnum> m = legendary ? Optional.of(ActiveEnum.BERSERK) : Optional.empty();
        final Optional<ActiveEnum> k = legendary ? Optional.of(ActiveEnum.KNOCKBACK) : Optional.empty();
        final Optional<ActiveEnum> t = legendary ? Optional.of(ActiveEnum.THORNS) : Optional.empty();
        final Optional<ActiveEnum> s = legendary ? Optional.of(ActiveEnum.SELF_HEAL) : Optional.empty();
        final Optional<ActiveEnum> c = legendary ? Optional.of(ActiveEnum.COUNTER_ATTACK) : Optional.empty();

        return List.of(
            item("mace", Set.of(PersonageSlot.MAIN_HAND),
                Optional.of(new ItemAttack(AttackType.BLUNT, 1, 298)),
                Optional.empty(), 0, 2, 1, 0.05, 15, 12, m),
            item("tower_shield", Set.of(PersonageSlot.OFF_HAND),
                Optional.empty(),
                Optional.of(new ItemDefense(DefenseType.PLATE, 32)),
                340, 0, 0, 0.0, 12, 8, k),
            item("cuirass", Set.of(PersonageSlot.BODY),
                Optional.empty(),
                Optional.of(new ItemDefense(DefenseType.PLATE, 64)),
                680, 0, 0, 0.0, 16, 12, t),
            item("greaves", Set.of(PersonageSlot.PANTS),
                Optional.empty(),
                Optional.of(new ItemDefense(DefenseType.PLATE, 36)),
                420, 0, 0, 0.0, 14, 8, s),
            item("sabatons", Set.of(PersonageSlot.SHOES),
                Optional.empty(),
                Optional.of(new ItemDefense(DefenseType.PLATE, 22)),
                280, 0, 0, 0.0, 10, 6, s),
            item("great_helm", Set.of(PersonageSlot.HELMET),
                Optional.empty(),
                Optional.of(new ItemDefense(DefenseType.PLATE, 22)),
                280, 0, 0, 0.0, 10, 6, c),
            item("gauntlets", Set.of(PersonageSlot.GLOVES),
                Optional.empty(),
                Optional.of(new ItemDefense(DefenseType.PLATE, 22)),
                280, 0, 0, 0.0, 10, 4, t)
        );
    }

    /**
     * DD: ARCANE set, longbow (PIERCE, range 3, occupies MAIN_HAND+OFF_HAND → 8 skill pts).
     * Legendary modifiers:
     *   Longbow → DOUBLE_ATTACK (8 pts → rank V),
     *   Wizard Robe → COUNTER_ATTACK, Arcane Chausses → COUNTER_ATTACK (stack → rank V),
     *   Arcane Boots → FEINT, Circlet → RETREAT,
     *   Arcane Gloves → THORNS
     */
    private static List<Item> ddGear(boolean legendary) {
        final Optional<ActiveEnum> da = legendary ? Optional.of(ActiveEnum.DOUBLE_ATTACK) : Optional.empty();
        final Optional<ActiveEnum> ca = legendary ? Optional.of(ActiveEnum.COUNTER_ATTACK) : Optional.empty();
        final Optional<ActiveEnum> fe = legendary ? Optional.of(ActiveEnum.FEINT) : Optional.empty();
        final Optional<ActiveEnum> re = legendary ? Optional.of(ActiveEnum.RETREAT) : Optional.empty();
        final Optional<ActiveEnum> th = legendary ? Optional.of(ActiveEnum.THORNS) : Optional.empty();

        return List.of(
            item("longbow", Set.of(PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND),
                Optional.of(new ItemAttack(AttackType.PIERCE, 3, 380)),
                Optional.empty(), 0, 3, 1, 0.05, 31, 5, da),
            item("wizard_robe", Set.of(PersonageSlot.BODY),
                Optional.empty(),
                Optional.of(new ItemDefense(DefenseType.ARCANE, 58)),
                510, 1, 1, 0.05, 32, 1, ca),
            item("arcane_chausses", Set.of(PersonageSlot.PANTS),
                Optional.empty(),
                Optional.of(new ItemDefense(DefenseType.ARCANE, 22)),
                280, 1, 1, 0.05, 28, 0, ca),
            item("arcane_boots", Set.of(PersonageSlot.SHOES),
                Optional.empty(),
                Optional.of(new ItemDefense(DefenseType.ARCANE, 14)),
                160, 1, 1, 0.05, 22, 0, fe),
            item("circlet", Set.of(PersonageSlot.HELMET),
                Optional.empty(),
                Optional.of(new ItemDefense(DefenseType.ARCANE, 14)),
                160, 4, 1, 0.10, 20, 1, re),
            item("arcane_gloves", Set.of(PersonageSlot.GLOVES),
                Optional.empty(),
                Optional.of(new ItemDefense(DefenseType.ARCANE, 14)),
                160, 1, 1, 0.05, 22, 0, th)
        );
    }

    // ----------------------------------------------------- team factory

    private static List<BattlePersonage> createTeam(boolean legendary) {
        return List.of(
            new BattlePersonage(tankGear(legendary), Position.FRONT),
            new BattlePersonage(ddGear(legendary), Position.BACK)
        );
    }

    // ---------------------------------------------------------- simulation

    private SimStats runIteration(RaidType raidType, int raidLevel, boolean legendary) {
        final var team = createTeam(legendary);
        final var tankId = team.getFirst().id();
        final var ddId = team.getLast().id();

        final var enemies = raidGenerator.generate(raidType, raidEvent(raidLevel), team);
        final var result = battle.process(enemies, team);

        final var tankStats = result.personageStats().get(tankId);
        final var ddStats = result.personageStats().get(ddId);

        final boolean playersWin = !result.firstWin();
        return new SimStats(
            playersWin ? 1 : 0,
            result.rounds(),
            tankStats.damageDealt(), ddStats.damageDealt(),
            tankStats.damageTaken(), ddStats.damageTaken(),
            tankStats.initialHealth() - tankStats.remainHealth(),
            ddStats.initialHealth() - ddStats.remainHealth(),
            tankStats.isDead() ? 0 : 1,
            ddStats.isDead() ? 0 : 1
        );
    }

    private SimStats runSimulation(RaidType raidType, int raidLevel, boolean legendary) {
        var stats = SimStats.zero();
        for (int i = 0; i < ITERATIONS; i++) {
            stats = stats.accumulate(runIteration(raidType, raidLevel, legendary));
        }
        return stats;
    }

    // ----------------------------------------------------------- output

    private static void printGearStats(boolean legendary) {
        final var team = createTeam(legendary);
        final var tank = team.getFirst();
        final var dd = team.getLast();
        System.out.printf("  Tank power: %.2f | DD power: %.2f%n", tank.power(), dd.power());
    }

    private static void printTable(SimStats[] results) {
        System.out.printf("%-17s | %-7s | %-7s | %-7s%n", "Metric", "Lvl 5", "Lvl 10", "Lvl 15");
        System.out.println("-".repeat(49));
        System.out.printf("%-17s | %6.1f%% | %6.1f%% | %6.1f%%%n",
            "Win Rate", results[0].winRate(), results[1].winRate(), results[2].winRate());
        System.out.printf("%-17s | %7.1f | %7.1f | %7.1f%n",
            "Avg Rounds", results[0].avgRounds(), results[1].avgRounds(), results[2].avgRounds());
        System.out.printf("%-17s | %7.0f | %7.0f | %7.0f%n",
            "Tank Dmg Dealt", results[0].avgTankDmgDealt(), results[1].avgTankDmgDealt(), results[2].avgTankDmgDealt());
        System.out.printf("%-17s | %7.0f | %7.0f | %7.0f%n",
            "DD Dmg Dealt", results[0].avgDdDmgDealt(), results[1].avgDdDmgDealt(), results[2].avgDdDmgDealt());
        System.out.printf("%-17s | %7.0f | %7.0f | %7.0f%n",
            "Tank HP Lost", results[0].avgTankHpLost(), results[1].avgTankHpLost(), results[2].avgTankHpLost());
        System.out.printf("%-17s | %7.0f | %7.0f | %7.0f%n",
            "DD HP Lost", results[0].avgDdHpLost(), results[1].avgDdHpLost(), results[2].avgDdHpLost());
        System.out.printf("%-17s | %6.1f%% | %6.1f%% | %6.1f%%%n",
            "Tank Survive", results[0].tankSurvive(), results[1].tankSurvive(), results[2].tankSurvive());
        System.out.printf("%-17s | %6.1f%% | %6.1f%% | %6.1f%%%n",
            "DD Survive", results[0].ddSurvive(), results[1].ddSurvive(), results[2].ddSurvive());
    }

    // ---------------------------------------------------------- utility

    private static LaunchedRaidEvent raidEvent(int raidLevel) {
        final var now = LocalDateTime.of(2026, 1, 1, 0, 0);
        return new LaunchedRaidEvent(
            1L, 1, now, now.plusHours(1),
            EventStatus.LAUNCHED,
            new RaidParams(raidLevel, 0)
        );
    }
}
