package ru.homyakin.seeker.game.event.raid.generator;

import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WolfPackGeneratorTest {
    private final WolfPackGenerator generator = new WolfPackGenerator();

    @Test
    void generateScalesWholePackToPersonagesPowerWithBonus() {
        final var personages = IntStream.range(0, 9)
            .mapToObj(ignored -> personage())
            .toList();
        final var powerBonus = 1.3;
        final var targetPower = personages.stream()
            .mapToDouble(BattlePersonage::power)
            .sum() * powerBonus;

        final var enemies = generator.generate(personages, powerBonus);
        final var enemiesPower = enemies.stream()
            .mapToDouble(BattlePersonage::power)
            .sum();

        assertTrue(
            Math.abs(enemiesPower - targetPower) / targetPower < 0.005,
            () -> "Expected enemy power close to " + targetPower + ", actual " + enemiesPower
        );
    }

    private BattlePersonage personage() {
        return new BattlePersonage(
            List.of(
                Item.fromObject(
                    new ItemObject(
                        null,
                        Set.of(PersonageSlot.MAIN_HAND),
                        Optional.of(new ItemAttack(AttackType.SLASH, 1, 1000)),
                        Optional.of(new ItemDefense(DefenseType.LEATHER, 500)),
                        5000,
                        5,
                        5,
                        0.5,
                        300,
                        10,
                        null
                    )
                )
            ),
            Position.FRONT
        );
    }
}
