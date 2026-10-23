package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;

class BattleContextBackwardMovementTest {
    @ParameterizedTest
    @CsvSource({
        "true, 0, 2, 1",
        "true, 1, 3, 2",
        "true, 2, 4, 3",
        "true, 3, 5, 4",
        "false, 0, 3, 4",
        "false, 1, 2, 3",
        "false, 2, 1, 2",
        "false, 3, 0, 1"
    })
    void oneStepRetreatsFromTheCurrentLineOnEitherHalf(
        boolean firstTeam,
        int advances,
        int expectedFrom,
        int expectedTo
    ) {
        final var fixture = fixture(firstTeam);
        for (int step = 0; step < advances; step++) {
            fixture.context().moveTowardEnemy(fixture.personage());
        }
        Assertions.assertEquals(expectedFrom, fixture.personage().currentPosition());

        fixture.context().moveBackward(fixture.personage(), 1);

        assertPlacement(fixture, expectedTo);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void aLargeRetreatClampsToTheFieldEdgeWithoutOverflow(boolean firstTeam) {
        final var fixture = fixture(firstTeam);
        fixture.context().moveTowardEnemy(fixture.personage());
        fixture.context().moveTowardEnemy(fixture.personage());

        fixture.context().moveBackward(fixture.personage(), Integer.MAX_VALUE);

        assertPlacement(fixture, firstTeam ? 0 : 5);
        fixture.context().moveBackward(fixture.personage(), 1);
        assertPlacement(fixture, firstTeam ? 0 : 5);
    }

    @ParameterizedTest
    @CsvSource({
        "true, 0", "true, -1", "true, -2147483648",
        "false, 0", "false, -1", "false, -2147483648"
    })
    void nonPositiveStepsDoNotMoveAPlacedPersonage(boolean firstTeam, int steps) {
        final var fixture = fixture(firstTeam);
        fixture.context().moveTowardEnemy(fixture.personage());
        fixture.context().moveTowardEnemy(fixture.personage());
        final int from = fixture.personage().currentPosition();

        fixture.context().moveBackward(fixture.personage(), steps);

        assertPlacement(fixture, from);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void aDefeatedPersonageDoesNotMove(boolean firstTeam) {
        final var fixture = fixture(firstTeam);
        fixture.context().moveTowardEnemy(fixture.personage());
        fixture.context().moveTowardEnemy(fixture.personage());
        final int from = fixture.personage().currentPosition();
        fixture.personage().applyVersionedSkillDamage(
            fixture.personage().health(),
            fixture.personage().health(),
            fixture.opponent()
        );
        Assertions.assertFalse(fixture.personage().isAlive());

        fixture.context().moveBackward(fixture.personage(), 1);

        assertPlacement(fixture, from);
    }

    @Test
    void anUnplacedPersonageDoesNotMove() {
        final var fixture = fixture(true);
        final var unplaced = personage();

        Assertions.assertDoesNotThrow(() -> fixture.context().moveBackward(unplaced, 1));

        Assertions.assertEquals(-1, unplaced.currentPosition());
        Assertions.assertTrue(fixture.context().lines().stream()
            .noneMatch(line -> line.battlePersonages().containsKey(unplaced.id())));
    }

    private static Fixture fixture(boolean firstTeam) {
        final var personage = personage();
        final var opponent = personage();
        final var context = new BattleContext(
            List.of(firstTeam ? personage : opponent),
            List.of(firstTeam ? opponent : personage),
            (sequence, minimum, maximum) -> minimum
        );
        return new Fixture(context, personage, opponent);
    }

    private static BattlePersonage personage() {
        return new BattlePersonage(
            List.of(new Item(
                new ItemObject(
                    null,
                    Set.of(),
                    List.of(new ItemAttack(AttackType.SLASH, 1, 1, 10)),
                    Optional.empty(),
                    1_000,
                    0,
                    0,
                    0,
                    100,
                    10,
                    0,
                    ItemProgressionVersion.V1,
                    Map.of()
                ),
                Optional.empty(),
                ItemRarity.COMMON
            )),
            Position.FRONT
        );
    }

    private static void assertPlacement(Fixture fixture, int expectedLine) {
        Assertions.assertEquals(expectedLine, fixture.personage().currentPosition());
        Assertions.assertSame(
            fixture.personage(),
            fixture.context().lines().get(expectedLine).battlePersonages().get(fixture.personage().id())
        );
        Assertions.assertEquals(1, fixture.context().lines().stream()
            .filter(line -> line.battlePersonages().containsKey(fixture.personage().id()))
            .count());
    }

    private record Fixture(BattleContext context, BattlePersonage personage, BattlePersonage opponent) {
    }
}
