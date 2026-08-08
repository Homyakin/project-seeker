package ru.homyakin.seeker.game.event.anomaly.generator;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveTemplate;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

import java.util.Optional;

class AnomalySafePveGeneratorTest {
    private final AnomalySafePveGenerator generator = new AnomalySafePveGenerator();

    @ParameterizedTest
    @MethodSource("enemyCounts")
    void enemyCountByPartySize(int players, int minEnemies, int maxEnemies) {
        final int count = AnomalySafePveGenerator.enemyCount(players);
        Assertions.assertTrue(count >= minEnemies && count <= maxEnemies);
    }

    @Test
    void templatesCoverAllAttackAndDefenseTypes() {
        final var attacks = Stream.of(AnomalyPveTemplate.values())
            .map(AnomalyPveTemplate::attackType)
            .collect(java.util.stream.Collectors.toSet());
        final var defenses = Stream.of(AnomalyPveTemplate.values())
            .map(AnomalyPveTemplate::defenseType)
            .collect(java.util.stream.Collectors.toSet());
        Assertions.assertEquals(Set.of(AttackType.values()), attacks);
        Assertions.assertEquals(Set.of(DefenseType.values()), defenses);
    }

    @Test
    void generateCreatesExpectedEnemyCount() {
        final var players = List.of(player(), player());
        final var enemies = generator.generate(AnomalyPveTemplate.CRYSTAL_STORM, players);
        Assertions.assertTrue(enemies.size() >= 2 && enemies.size() <= 3);
        Assertions.assertTrue(enemies.stream().allMatch(it ->
            it.attackTypes().contains(AttackType.MAGICAL)
        ));
    }

    private static Stream<Arguments> enemyCounts() {
        return Stream.of(
            Arguments.of(1, 2, 2),
            Arguments.of(2, 2, 3),
            Arguments.of(3, 3, 3),
            Arguments.of(4, 4, 4),
            Arguments.of(5, 5, 5)
        );
    }

    private BattlePersonage player() {
        return new BattlePersonage(
            List.of(
                new Item(
                    new ItemObject(
                        null,
                        Set.of(PersonageSlot.MAIN_HAND),
                        Optional.of(new ItemAttack(AttackType.SLASH, 1, 100)),
                        Optional.of(new ItemDefense(DefenseType.LEATHER, 100)),
                        300,
                        5,
                        5,
                        0.4,
                        180,
                        20,
                        null
                    ),
                    Optional.empty(),
                    ItemRarity.COMMON
                )
            ),
            Position.FRONT
        );
    }
}
