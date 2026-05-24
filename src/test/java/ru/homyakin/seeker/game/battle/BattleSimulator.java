package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.Battle;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.models.ItemRarity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Disabled
public class BattleSimulator {
    // 🔨 Воин
    // HP=2500, DEF=320 PLATE, ATK=150 BLUNT
    private static final List<Item> warriorItems = List.of(
        Item.weapon(AttackType.SLASH, 1, 150, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON),
        Item.armor (DefenseType.PLATE, 200, 800, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON),
        Item.armor (DefenseType.PLATE,  50, 500, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON),
        Item.armor (DefenseType.PLATE,  40, 500, new Modifier(ActiveEnum.SELF_HEAL), ItemRarity.COMMON),
        Item.armor (DefenseType.PLATE,  15, 350, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON),
        Item.armor (DefenseType.PLATE,  15, 350, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON)
    );
    // 🔨 Воин
    // HP=2500, DEF=320 PLATE, ATK=150 BLUNT
    private static final List<Item> warriorSkillItems = List.of(
        Item.weapon(AttackType.SLASH, 1, 150, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON),
        Item.armor (DefenseType.PLATE, 200, 800, new Modifier(ActiveEnum.BLEEDING), ItemRarity.LEGENDARY),
        Item.armor (DefenseType.PLATE,  50, 500, new Modifier(ActiveEnum.BLEEDING), ItemRarity.LEGENDARY),
        Item.armor (DefenseType.PLATE,  40, 500, new Modifier(ActiveEnum.SELF_HEAL), ItemRarity.COMMON),
        Item.armor (DefenseType.PLATE,  15, 350, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON),
        Item.armor (DefenseType.PLATE,  15, 350, new Modifier(ActiveEnum.THORNS), ItemRarity.COMMON)
    );

    // COUNTER_ATTACK - (90.097%) + 100714.81549313999
    // THORNS - 90.635% + 100714.81549313999
    // DOUBLE_ATTACK - 89.27% 100714.81549313999
    // BERSERK - 90.131% 100265.19578111706
    // BLEEDING - 66.293% 95409.30289126924
    // SELF_HEAL - 90.403% 96668.23808493349
    // PRECISE_STRIKE - 52.711% 92621.66067672695
    // FEINT - 53.817% 92891.43250394074

    // 🗹 Ассасин
    // HP=600, DEF=80 LEATHER, ATK=380 PIERCE
    private static final List<Item> assassinItems = List.of(
        Item.weapon(AttackType.PIERCE,         1, 380, new Modifier(ActiveEnum.BLEEDING), ItemRarity.COMMON),
        Item.armor (DefenseType.LEATHER, 50, 300, new Modifier(ActiveEnum.FEINT), ItemRarity.COMMON),
        Item.armor (DefenseType.LEATHER, 15, 150, new Modifier(ActiveEnum.FEINT), ItemRarity.COMMON),
        Item.armor (DefenseType.LEATHER, 15, 150, new Modifier(ActiveEnum.FEINT), ItemRarity.COMMON)
    );

    // 🔮 Маг
    // HP=950, DEF=80 CLOTH, ATK=380 MAGICAL
    private static final List<Item> mageItems = List.of(
        Item.weapon(AttackType.MAGICAL,          3, 380, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON),
        Item.armor (DefenseType.CLOTH,    50, 600, new Modifier(ActiveEnum.SELF_HEAL), ItemRarity.COMMON),
        Item.armor (DefenseType.CLOTH,    15, 200, new Modifier(ActiveEnum.SELF_HEAL), ItemRarity.COMMON),
        Item.armor (DefenseType.CLOTH,    15, 150, new Modifier(ActiveEnum.SELF_HEAL), ItemRarity.COMMON)
    );

    private static final List<Item> archerItems = List.of(
        Item.weapon(AttackType.PIERCE,          2, 380, new Modifier(ActiveEnum.PRECISE_STRIKE), ItemRarity.COMMON),
        Item.armor (DefenseType.CLOTH,    50, 300, new Modifier(ActiveEnum.RETREAT), ItemRarity.COMMON),
        Item.armor (DefenseType.CLOTH,    15, 150, new Modifier(ActiveEnum.RETREAT), ItemRarity.COMMON),
        Item.armor (DefenseType.CLOTH,    15, 150, new Modifier(ActiveEnum.SELF_HEAL), ItemRarity.COMMON)
    );

    private static List<Item> withPersonageStats(
        List<Item> items,
        int critChance,
        int dodgeChance,
        double critMultiplier,
        int speed,
        int baseThreat
    ) {
        final var list = new ArrayList<>(items);
        list.add(Item.stats(critChance, dodgeChance, critMultiplier, speed, baseThreat));
        return List.copyOf(list);
    }

    private static BattlePersonage warrior(Position position) {
        return new BattlePersonage(
            withPersonageStats(warriorItems, 5, 5, 1.5, 140, 100),
            position
        );
    }

    private static BattlePersonage warriorSkillItems(Position position) {
        return new BattlePersonage(
            withPersonageStats(BattleSimulator.warriorSkillItems, 5, 5, 1.5, 140, 100),
            position
        );
    }

    private static BattlePersonage mage(Position position) {
        return new BattlePersonage(
            withPersonageStats(mageItems, 15, 10, 1.75, 180, 5),
            position
        );
    }

    private static BattlePersonage assassin(Position position) {
        return new BattlePersonage(
            withPersonageStats(assassinItems, 20, 25, 2.0, 220, 10),
            position
        );
    }

    private static BattlePersonage archer() {
        return new BattlePersonage(
            withPersonageStats(archerItems, 20, 25, 2.0, 220, 10),
            Position.MID
        );
    }


    @Test
    public void simple() {
        int iterations = 100_000;
        int firstWins = 0;
        int totalRounds = 0;
        int maxRounds = 0;
        long sumFirstDamageTaken = 0;
        long sumFirstDefenceEvents = 0;
        long sumSecondDamageTaken = 0;
        long sumSecondDefenceEvents = 0;
        int winsFirstAlive = 0;
        int winsSecondAlive = 0;
        int winsBothAlive = 0;


        System.out.println("WARRIOR " + warrior(Position.FRONT).power());
        System.out.println("WARRIOR SKILL " + warriorSkillItems(Position.FRONT).power());

        for (int i = 0; i < iterations; ++i) {
            final var firstTeam = List.of(warriorSkillItems(Position.FRONT));
            final var secondTeam = List.of(warrior(Position.FRONT));

            //System.out.println(firstTeam.getFirst().power());
            //System.out.println(secondTeam.getFirst().power());

            var result = new Battle().process(firstTeam, secondTeam);

            if (result.firstWin()) {
                firstWins++;
                final boolean firstAlive = firstTeam.getFirst().isAlive();
                /*
                final boolean secondAlive = firstTeam.get(1).isAlive();
                if (firstAlive && !secondAlive) {
                    winsFirstAlive++;
                }
                if (secondAlive && !firstAlive) {
                    winsSecondAlive++;
                }
                if (firstAlive && secondAlive) {
                    winsBothAlive++;
                }
                 */
            }
            totalRounds += result.rounds();
            maxRounds = Math.max(maxRounds, result.rounds());
/*
            final var s0 = firstTeam.getFirst().battlePersonageStats();
            final var s1 = firstTeam.get(1).battlePersonageStats();
            sumFirstDamageTaken += s0.damageBlocked() + s0.damageDodged();
            sumFirstDefenceEvents += s0.blockCount() + s0.dodgesCount();
            sumSecondDamageTaken += s1.damageBlocked() + s1.damageDodged();
            sumSecondDefenceEvents += s1.blockCount() + s1.dodgesCount();

 */
        }

        System.out.println("First wins:   " + firstWins + " / " + iterations +
            " (" + (firstWins * 100.0 / iterations) + "%)");
        if (firstWins > 0) {
            System.out.printf(
                "When first wins — 1st alive: %.2f%%, 2nd alive: %.2f%%, both alive: %.2f%%%n",
                winsFirstAlive * 100.0 / firstWins,
                winsSecondAlive * 100.0 / firstWins,
                winsBothAlive * 100.0 / firstWins
            );
        } else {
            System.out.println("When first wins — (no first-team wins in sample)");
        }
        System.out.println("Avg rounds:   " + totalRounds / iterations);
        System.out.println("Max rounds:   " + maxRounds);
        System.out.printf(
            "Team1 1st (tank):     avg incoming dmg (block+dodge rolls)=%.2f, avg block+dodge events=%.2f%n",
            sumFirstDamageTaken / (double) iterations,
            sumFirstDefenceEvents / (double) iterations
        );
        System.out.printf(
            "Team1 2nd (assassin): avg incoming dmg (block+dodge rolls)=%.2f, avg block+dodge events=%.2f%n",
            sumSecondDamageTaken / (double) iterations,
            sumSecondDefenceEvents / (double) iterations
        );
    }


    @Test
    public void power() {
        System.out.println("WARRIOR " + warrior(Position.FRONT).power());
        // System.out.println("WARRIOR SKILL " + warriorSkillItems(Position.FRONT).power());
        System.out.println("MAGE " + mage(Position.FRONT).power());
        System.out.println("ASSASIN " + assassin(Position.FRONT).power());
        System.out.println("ARCHER " + archer().power());
    }

    @Test
    public void logTest() throws Exception {
        final var firstTeam = List.of(warrior(Position.FRONT), warrior(Position.FRONT), warrior(Position.FRONT), mage(Position.BACK));
        final var secondTeam = List.of(warrior(Position.FRONT), warrior(Position.FRONT), archer(), mage(Position.BACK));

        final var result = new Battle().process(firstTeam, secondTeam);
        final var mapper = JsonMapper.builder()
            .addModule(new Jdk8Module())
            .build();
        final var writer = mapper.writerWithDefaultPrettyPrinter();
        final var outDir = Path.of("target", "battle-v4-log");
        Files.createDirectories(outDir);
        final var initPath = outDir.resolve("battle-init.json");
        final var logPath = outDir.resolve("battle-action-log.json");
        writer.writeValue(initPath.toFile(), result.initState());
        writer.writeValue(logPath.toFile(), result.actionLog().events());
    }
}
