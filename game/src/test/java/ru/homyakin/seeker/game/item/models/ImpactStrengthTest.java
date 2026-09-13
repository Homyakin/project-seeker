package ru.homyakin.seeker.game.item.models;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

class ImpactStrengthTest {

    @Test
    void emptyEquipmentHasBaseImpactStrength() {
        Assertions.assertEquals(80, ImpactStrength.fromItems(List.of()));
    }

    @Test
    void contributionsAreAddedAcrossAllItems() {
        Assertions.assertEquals(
            90,
            ImpactStrength.fromItems(List.of(item(4, 0), item(6, 0)))
        );
    }

    @Test
    void resultIsCappedAtOneHundred() {
        Assertions.assertAll(
            () -> Assertions.assertEquals(100, ImpactStrength.fromItems(List.of(item(20, 0)))),
            () -> Assertions.assertEquals(100, ImpactStrength.fromItems(List.of(item(19, 0), item(2, 0)))),
            () -> Assertions.assertEquals(100, ImpactStrength.fromItems(List.of(item(Integer.MAX_VALUE, 0))))
        );
    }

    @Test
    void enhancementRarityAndModifierDoNotChangeContribution() {
        final var object = object(10);
        final var base = new Item(object, Optional.empty(), ItemRarity.COMMON, 0);
        final var enhanced = new Item(
            object,
            Optional.of(new Modifier(ActiveEnum.BERSERK)),
            ItemRarity.LEGENDARY,
            20
        );

        Assertions.assertEquals(90, ImpactStrength.fromItems(List.of(base)));
        Assertions.assertEquals(90, ImpactStrength.fromItems(List.of(enhanced)));
    }

    @Test
    void negativeContributionFailsExplicitly() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> ImpactStrength.fromItems(List.of(item(-1, 0)))
        );
    }

    private static Item item(int impact, int enhanceLevel) {
        return new Item(object(impact), Optional.empty(), ItemRarity.COMMON, enhanceLevel);
    }

    private static ItemObject object(int impact) {
        return new ItemObject(
            "impact-test",
            Set.of(PersonageSlot.MAIN_HAND),
            List.of(),
            Optional.empty(),
            0,
            0,
            0,
            0,
            0,
            0,
            impact,
            ItemProgressionVersion.V1,
            Map.of()
        );
    }
}
