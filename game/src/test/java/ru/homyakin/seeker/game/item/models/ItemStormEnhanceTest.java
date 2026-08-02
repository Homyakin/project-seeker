package ru.homyakin.seeker.game.item.models;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;

public class ItemStormEnhanceTest {

    @Test
    void Given_EnhanceLevel_When_VisibleCharacteristics_Then_AppliesFivePercentPerLevel() {
        final var item = baseItem(100, 50, 20, 2);

        Assertions.assertEquals(110, item.health());
        Assertions.assertEquals(55, item.itemAttack().orElseThrow().attack());
        Assertions.assertEquals(22, item.itemDefense().orElseThrow().defense());
    }

    @Test
    void Given_EnhanceLevel_When_WithoutStormEnhance_Then_ReturnsBaseStats() {
        final var item = baseItem(100, 50, 20, 3).withoutStormEnhance();

        Assertions.assertEquals(100, item.health());
        Assertions.assertEquals(50, item.itemAttack().orElseThrow().attack());
        Assertions.assertEquals(20, item.itemDefense().orElseThrow().defense());
        Assertions.assertEquals(0, item.enhanceLevel());
    }

    private static Item baseItem(int health, int attack, int defense, int enhanceLevel) {
        return new Item(
            new ItemObject(
                null,
                Set.of(),
                Optional.of(new ItemAttack(AttackType.SLASH, 1, attack)),
                Optional.of(new ItemDefense(DefenseType.PLATE, defense)),
                health,
                0,
                0,
                0,
                0,
                0,
                null
            ),
            Optional.of(new Modifier(ActiveEnum.THORNS)),
            ItemRarity.RARE,
            enhanceLevel
        );
    }
}
