package ru.homyakin.seeker.game.item.characteristics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.item.characteristics.models.ModifierCharacteristicType;
import ru.homyakin.seeker.game.item.characteristics.models.ModifierGenerateCharacteristics;
import ru.homyakin.seeker.game.item.characteristics.models.ObjectCharacteristicType;
import ru.homyakin.seeker.game.item.characteristics.models.ObjectGenerateCharacteristics;
import ru.homyakin.seeker.game.item.models.LegacyGenerateItemObject;
import ru.homyakin.seeker.game.item.modifier.models.LegacyGenerateModifier;
import ru.homyakin.seeker.game.item.models.LegacyItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ItemCharacteristicServiceTest {
    private final LegacyItemCharacteristicConfig config = new LegacyItemCharacteristicConfig();
    private final LegacyItemCharacteristicService service = new LegacyItemCharacteristicService(config);

    @BeforeEach
    public void init() {
        config.setBaseAttack(12);
        config.setBaseDefense(12);
        config.setBaseHealth(12);
        config.setModifierImpact(0.5);
        config.setDeviation(0.0);
    }

    @Test
    void Given_ObjectWithAttackAndOneSlot_When_CreateCharacteristics_Then_CharacteristicsWithBaseAttack() {
        final var rarity = Mockito.mock(LegacyItemRarity.class);
        final var object = Mockito.mock(LegacyGenerateItemObject.class);
        final var characteristics = new ObjectGenerateCharacteristics(List.of(ObjectCharacteristicType.ATTACK));

        Mockito.when(rarity.multiplier()).thenReturn(1.0);
        Mockito.when(object.characteristics()).thenReturn(characteristics);
        Mockito.when(object.slots()).thenReturn(Set.of(PersonageSlot.MAIN_HAND));

        final var result = service.createCharacteristics(rarity, object, Collections.emptyList());

        Assertions.assertEquals(0, result.health());
        Assertions.assertEquals(12, result.attack());
        Assertions.assertEquals(0, result.defense());
    }

    @Test
    void Given_ObjectWithAttackDefenseHealthAndOneSlot_When_CreateCharacteristics_Then_CharacteristicsWithBaseAttackDefenseHealthDivideBy3() {
        final var rarity = Mockito.mock(LegacyItemRarity.class);
        final var object = Mockito.mock(LegacyGenerateItemObject.class);
        final var characteristics = new ObjectGenerateCharacteristics(
            List.of(ObjectCharacteristicType.ATTACK, ObjectCharacteristicType.DEFENSE, ObjectCharacteristicType.HEALTH)
        );

        Mockito.when(rarity.multiplier()).thenReturn(1.0);
        Mockito.when(object.characteristics()).thenReturn(characteristics);
        Mockito.when(object.slots()).thenReturn(Set.of(PersonageSlot.MAIN_HAND));

        final var result = service.createCharacteristics(rarity, object, Collections.emptyList());

        Assertions.assertEquals(4, result.health());
        Assertions.assertEquals(4, result.attack());
        Assertions.assertEquals(4, result.defense());
    }

    @Test
    void Given_ObjectWithAttackAndTwoSlots_When_CreateCharacteristics_Then_CharacteristicsWithBaseAttackMultiply2() {
        final var rarity = Mockito.mock(LegacyItemRarity.class);
        final var object = Mockito.mock(LegacyGenerateItemObject.class);
        final var characteristics = new ObjectGenerateCharacteristics(List.of(ObjectCharacteristicType.ATTACK));

        Mockito.when(rarity.multiplier()).thenReturn(1.0);
        Mockito.when(object.characteristics()).thenReturn(characteristics);
        Mockito.when(object.slots()).thenReturn(Set.of(PersonageSlot.MAIN_HAND, PersonageSlot.GLOVES));

        final var result = service.createCharacteristics(rarity, object, Collections.emptyList());

        Assertions.assertEquals(0, result.health());
        Assertions.assertEquals(24, result.attack());
        Assertions.assertEquals(0, result.defense());
    }

    @Test
    void Given_ObjectWithAttackAndOneSlotAndOneModifierWithAttack_When_CreateCharacteristics_Then_CharacteristicsWithBaseAttackPlusModifierImpact() {
        final var rarity = Mockito.mock(LegacyItemRarity.class);
        final var object = Mockito.mock(LegacyGenerateItemObject.class);
        final var modifier1 = Mockito.mock(LegacyGenerateModifier.class);
        final var objectCharacteristics = new ObjectGenerateCharacteristics(List.of(ObjectCharacteristicType.ATTACK));
        final var modifierCharacteristics = new ModifierGenerateCharacteristics(List.of(ModifierCharacteristicType.ATTACK));

        Mockito.when(rarity.multiplier()).thenReturn(1.0);
        Mockito.when(object.characteristics()).thenReturn(objectCharacteristics);
        Mockito.when(object.slots()).thenReturn(Set.of(PersonageSlot.MAIN_HAND));
        Mockito.when(modifier1.characteristics()).thenReturn(modifierCharacteristics);

        final var result = service.createCharacteristics(rarity, object, List.of(modifier1));

        Assertions.assertEquals(0, result.health());
        Assertions.assertEquals(18, result.attack());
        Assertions.assertEquals(0, result.defense());
    }

    @Test
    void Given_ObjectWithAttackAndOneSlotAndOneModifierWithDefense_When_CreateCharacteristics_Then_CharacteristicsWithBaseAttackModifierDefense() {
        final var rarity = Mockito.mock(LegacyItemRarity.class);
        final var object = Mockito.mock(LegacyGenerateItemObject.class);
        final var modifier1 = Mockito.mock(LegacyGenerateModifier.class);
        final var objectCharacteristics = new ObjectGenerateCharacteristics(List.of(ObjectCharacteristicType.ATTACK));
        final var modifierCharacteristics = new ModifierGenerateCharacteristics(List.of(ModifierCharacteristicType.DEFENSE));

        Mockito.when(rarity.multiplier()).thenReturn(1.0);
        Mockito.when(object.characteristics()).thenReturn(objectCharacteristics);
        Mockito.when(object.slots()).thenReturn(Set.of(PersonageSlot.MAIN_HAND));
        Mockito.when(modifier1.characteristics()).thenReturn(modifierCharacteristics);

        final var result = service.createCharacteristics(rarity, object, List.of(modifier1));

        Assertions.assertEquals(0, result.health());
        Assertions.assertEquals(12, result.attack());
        Assertions.assertEquals(6, result.defense());
    }

    @Test
    void Given_ObjectWithAttackAndOneSlotAndOneModifierWithDefenseAndMultiplier_When_CreateCharacteristics_Then_CharacteristicsWithBaseAttackModifierDefenseMultiplyModifier() {
        final var rarity = Mockito.mock(LegacyItemRarity.class);
        final var object = Mockito.mock(LegacyGenerateItemObject.class);
        final var modifier1 = Mockito.mock(LegacyGenerateModifier.class);
        final var objectCharacteristics = new ObjectGenerateCharacteristics(List.of(ObjectCharacteristicType.ATTACK));
        final var modifierCharacteristics = new ModifierGenerateCharacteristics(List.of(ModifierCharacteristicType.DEFENSE, ModifierCharacteristicType.MULTIPLIER));

        Mockito.when(rarity.multiplier()).thenReturn(1.0);
        Mockito.when(object.characteristics()).thenReturn(objectCharacteristics);
        Mockito.when(object.slots()).thenReturn(Set.of(PersonageSlot.MAIN_HAND));
        Mockito.when(modifier1.characteristics()).thenReturn(modifierCharacteristics);

        final var result = service.createCharacteristics(rarity, object, List.of(modifier1));

        Assertions.assertEquals(0, result.health());
        Assertions.assertEquals(15, result.attack());
        Assertions.assertEquals(4, result.defense());
    }

    @Test
    void Given_SmallAttackAndObjectWithAttackAndOneSlotAndOneModifierWithMultiplier_When_CreateCharacteristics_Then_ModifierImpactIsOne() {
        config.setBaseAttack(1);
        config.setModifierImpact(0.1);
        final var rarity = Mockito.mock(LegacyItemRarity.class);
        final var object = Mockito.mock(LegacyGenerateItemObject.class);
        final var modifier1 = Mockito.mock(LegacyGenerateModifier.class);
        final var objectCharacteristics = new ObjectGenerateCharacteristics(List.of(ObjectCharacteristicType.ATTACK));
        final var modifierCharacteristics = new ModifierGenerateCharacteristics(List.of(ModifierCharacteristicType.MULTIPLIER));

        Mockito.when(rarity.multiplier()).thenReturn(1.0);
        Mockito.when(object.characteristics()).thenReturn(objectCharacteristics);
        Mockito.when(object.slots()).thenReturn(Set.of(PersonageSlot.MAIN_HAND));
        Mockito.when(modifier1.characteristics()).thenReturn(modifierCharacteristics);

        final var result = service.createCharacteristics(rarity, object, List.of(modifier1));

        Assertions.assertEquals(0, result.health());
        Assertions.assertEquals(2, result.attack());
        Assertions.assertEquals(0, result.defense());
    }
}
