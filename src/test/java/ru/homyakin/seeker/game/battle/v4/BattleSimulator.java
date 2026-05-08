package ru.homyakin.seeker.game.battle.v4;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class BattleSimulator {
    // 🔨 Воин
    // HP=2500, DEF=320 PLATE, ATK=150 BLUNT
    private static final List<Item> warriorItems = List.of(
        Item.weapon(AttackType.BLUNT,          1, 150),
        Item.armor (DefenseType.PLATE, 200, 800),
        Item.armor (DefenseType.PLATE,  50, 500),
        Item.armor (DefenseType.PLATE,  40, 500),
        Item.armor (DefenseType.PLATE,  15, 350),
        Item.armor (DefenseType.PLATE,  15, 350)
    );

    // 🗹 Ассасин
    // HP=600, DEF=80 LEATHER, ATK=380 PIERCE
    private static final List<Item> assassinItems = List.of(
        Item.weapon(AttackType.PIERCE,         1, 380),
        Item.armor (DefenseType.LEATHER, 50, 300),
        Item.armor (DefenseType.LEATHER, 15, 150),
        Item.armor (DefenseType.LEATHER, 15, 150)
    );

    // 🔮 Маг
    // HP=950, DEF=80 CLOTH, ATK=380 MAGICAL
    private static final List<Item> mageItems = List.of(
        Item.weapon(AttackType.MAGICAL,          3, 380),
        Item.armor (DefenseType.CLOTH,    50, 600), // ← было 500
        Item.armor (DefenseType.CLOTH,    15, 200),
        Item.armor (DefenseType.CLOTH,    15, 150)
    );

    private static BattlePersonage warrior(Position position) {
        return new BattlePersonage(
            warriorItems,
            5,     // critChance
            5,     // dodgeChance
            1.5,   // critMultiplier
            140,   // initiative
            100,   // baseThreat
            position
        );
    }

    private static BattlePersonage mage(Position position) {
        return new BattlePersonage(
            mageItems,
            // HP=850, DEF=80 CLOTH, ATK=380 MAGICAL, range=3
            15,    // critChance
            10,    // dodgeChance
            1.75,  // critMultiplier
            180,   // initiative
            5,     // baseThreat
            position
        );
    }

    private static BattlePersonage warriorMixed(Position position) {
        return new BattlePersonage(
            List.of(
                Item.weapon(AttackType.BLUNT,          1, 100),
                Item.weapon(AttackType.PIERCE,         1,  50),
                Item.armor (DefenseType.PLATE, 200, 800),
                Item.armor (DefenseType.PLATE,  50, 500),
                Item.armor (DefenseType.PLATE,  40, 500),
                Item.armor (DefenseType.PLATE,  15, 350),
                Item.armor (DefenseType.PLATE,  15, 350)
            ),
            // HP=2500, DEF=320 PLATE, ATK=100 BLUNT + 50 PIERCE
            5,     // critChance
            5,     // dodgeChance
            1.5,   // critMultiplier
            140,   // initiative
            100,   // baseThreat
            position
        );
    }

    private static BattlePersonage assassin(Position position) {
        return new BattlePersonage(
            assassinItems,
            20,    // critChance
            25,    // dodgeChance
            2.0,   // critMultiplier
            220,   // initiative
            10,    // baseThreat
            position
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

        for (int i = 0; i < iterations; ++i) {
            final var firstTeam = List.of(assassin(Position.FRONT), warrior(Position.FRONT), warrior(Position.FRONT), mage(Position.BACK));
            final var secondTeam = List.of(warrior(Position.FRONT), warrior(Position.FRONT), warrior(Position.FRONT), mage(Position.BACK));

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
        System.out.println("MAGE " + mage(Position.FRONT).power());
        System.out.println("ASSASIN " + assassin(Position.FRONT).power());
        System.out.println("WARRIOR MIX " + warriorMixed(Position.FRONT).power());
    }

    @Test
    public void logTest() throws Exception {
        final var result = new Battle().process(List.of(), List.of());
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

    // 🔨 Воин
    List<Item> warrior = List.of(
        Item.weapon(AttackType.BLUNT,          1, 150),
        Item.armor (DefenseType.PLATE, 200, 800),
        Item.armor (DefenseType.PLATE,  50, 500),
        Item.armor (DefenseType.PLATE,  40, 500),
        Item.armor (DefenseType.PLATE,  15, 350),
        Item.armor (DefenseType.PLATE,  15, 350)
    );

    // 🗹 Ассасин
    List<Item> assassin = List.of(
        Item.weapon(AttackType.PIERCE,         1, 380),
        Item.armor (DefenseType.LEATHER, 50, 300),
        Item.armor (DefenseType.LEATHER, 15, 150),
        Item.armor (DefenseType.LEATHER, 15, 150)
    );

    // 🔮 Маг
    List<Item> mage = List.of(
        Item.weapon(AttackType.MAGICAL,        3, 380),
        Item.armor (DefenseType.CLOTH,   50, 500),
        Item.armor (DefenseType.CLOTH,   15, 200),
        Item.armor (DefenseType.CLOTH,   15, 150)
    );
}
