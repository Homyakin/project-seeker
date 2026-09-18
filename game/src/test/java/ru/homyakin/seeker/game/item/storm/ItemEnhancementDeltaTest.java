package ru.homyakin.seeker.game.item.storm;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

class ItemEnhancementDeltaTest {
    @Test
    void v1ReportsExactIncreaseForEveryGrowingPart() {
        final var object = new ItemObject(
            "hybrid",
            Set.of(PersonageSlot.MAIN_HAND),
            List.of(
                new ItemAttack(AttackType.SLASH, 1, 1, 60),
                new ItemAttack(AttackType.MAGICAL, 2, 3, 120)
            ),
            Optional.of(new ItemDefense(DefenseType.ARCANE, 180)),
            240,
            0,
            0,
            0,
            0,
            0,
            0,
            ItemProgressionVersion.V1,
            Map.of()
        );

        final var delta = ItemEnhancementDelta.next(object, 10);

        Assertions.assertEquals(Optional.of(4), delta.health());
        Assertions.assertEquals(
            List.of(
                new ItemEnhancementDelta.AttackDelta(AttackType.SLASH, 1, 1, 1),
                new ItemEnhancementDelta.AttackDelta(AttackType.MAGICAL, 2, 3, 2)
            ),
            delta.attacks()
        );
        Assertions.assertEquals(
            Optional.of(new ItemEnhancementDelta.DefenseDelta(DefenseType.ARCANE, 3)),
            delta.defense()
        );
        Assertions.assertTrue(delta.hasChanges());
    }

    @Test
    void legacyItemUsesItsOwnConfiguredProgression() {
        final var object = new ItemObject(
            "legacy",
            Set.of(PersonageSlot.BODY),
            Optional.empty(),
            Optional.of(new ItemDefense(DefenseType.CLOTH, 20)),
            100,
            0,
            0,
            0,
            0,
            0,
            Map.of()
        );

        final var delta = ItemEnhancementDelta.next(object, 0);

        Assertions.assertEquals(Optional.of(5), delta.health());
        Assertions.assertEquals(
            Optional.of(new ItemEnhancementDelta.DefenseDelta(DefenseType.CLOTH, 1)),
            delta.defense()
        );
        Assertions.assertTrue(delta.hasChanges());
    }

    @Test
    void unchangedLegacyTransitionIsReportedExplicitly() {
        final var object = new ItemObject(
            "tiny",
            Set.of(PersonageSlot.BODY),
            Optional.empty(),
            Optional.empty(),
            1,
            0,
            0,
            0,
            0,
            0,
            Map.of()
        );

        final var delta = ItemEnhancementDelta.next(object, 0);

        Assertions.assertEquals(Optional.of(0), delta.health());
        Assertions.assertFalse(delta.hasChanges());
    }
}
