package ru.homyakin.seeker.game.item.catalog;

import java.util.List;
import java.util.Optional;
import java.util.function.ToLongFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.homyakin.seeker.game.battle.CombatRules;
import ru.homyakin.seeker.game.battle.skill.scaling.NonNegativeRational;
import ru.homyakin.seeker.game.battle.skill.scaling.ScalingSkillMath;
import ru.homyakin.seeker.game.item.models.ImpactStrength;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.storm.ItemProgression;
import ru.homyakin.seeker.game.item.storm.StormEnhanceConfig;
import ru.homyakin.seeker.game.item.storm.StormEnhanceTechnicalLimit;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.power.PowerDisplayVersion;

/** Bounds only the selected equipment release, not arbitrary NPCs, effects or persisted out-of-range levels. */
class EquipmentReleaseNumericDomainTest {
    private static final List<ItemObject> OBJECTS = Stream.of(
        EquipmentCatalogVersion.SCALING_V4, EquipmentCatalogVersion.SCALING_V5
    ).map(EquipmentCatalogLoader::loadValidated)
        .flatMap(catalog -> Stream.concat(catalog.itemObjects().stream(), catalog.defaultItems().stream()))
        .toList();
    private static final int MAX_LEVEL = 75;

    @ParameterizedTest(name = "All 70 objects of each V4/V5 release are readable and monotone at +{0}")
    @MethodSource("levels")
    void everyObjectHasReadableMonotoneStatesThroughoutTheSupportedRange(int level) {
        Assertions.assertEquals(140, OBJECTS.size());
        for (final var object : OBJECTS) {
            final var item = item(object, level);
            final var previous = item(object, Math.max(0, level - 1));
            final var state = Assertions.assertDoesNotThrow(() -> ItemProgression.state(object, level), object.code());
            final var visible = Assertions.assertDoesNotThrow(item::visibleCharacteristics, object.code());
            Assertions.assertEquals(independentState(object.health(), level), item.health(), object.code());
            Assertions.assertEquals(state.health(), visible.health(), object.code());
            Assertions.assertTrue(item.health() >= previous.health(), object.code());
            Assertions.assertEquals(state.attacks(), item.itemAttacks(), object.code());
            Assertions.assertEquals(state.defense(), item.itemDefense(), object.code());
            for (int index = 0; index < object.attacks().size(); index++) {
                final var part = item.itemAttacks().get(index);
                final var before = previous.itemAttacks().get(index);
                final var base = object.attacks().get(index);
                Assertions.assertEquals(independentState(base.attack(), level), part.attack(), object.code());
                Assertions.assertTrue(part.attack() >= before.attack(), object.code());
                Assertions.assertEquals(base.attackType(), part.attackType());
                Assertions.assertEquals(base.minRange(), part.minRange());
                Assertions.assertEquals(base.maxRange(), part.maxRange());
            }
            if (object.defense().isPresent()) {
                Assertions.assertEquals(
                    independentState(object.defense().orElseThrow().defense(), level),
                    item.itemDefense().orElseThrow().defense(), object.code()
                );
                Assertions.assertTrue(
                    item.itemDefense().orElseThrow().defense() >= previous.itemDefense().orElseThrow().defense(), object.code()
                );
            }
            Assertions.assertEquals(object.speed(), item.speed());
            Assertions.assertEquals(object.critChance(), item.critChance());
            Assertions.assertEquals(object.dodgeChance(), item.dodgeChance());
            Assertions.assertEquals(object.critMultiplier(), item.critMultiplier());
            Assertions.assertEquals(object.impact(), item.impact());
            Assertions.assertEquals(object.baseThreat(), item.baseThreat());
        }
    }

    @Test
    void actualPriceAndProgressionLimitsAllowState75AndRejectTheNextAttemptForEveryObject() {
        final var config = new StormEnhanceConfig();
        final var technicalLimit = new StormEnhanceTechnicalLimit(config);
        Assertions.assertEquals(MAX_LEVEL, config.maxPriceSupportedStateLevel());
        for (final var object : OBJECTS) {
            Assertions.assertEquals(MAX_LEVEL, technicalLimit.maxStateLevel(object), object.code());
            Assertions.assertDoesNotThrow(() -> ItemProgression.state(object, MAX_LEVEL), object.code());
            Assertions.assertDoesNotThrow(() -> config.costForLevel(MAX_LEVEL - 1, object.slots()), object.code());
            Assertions.assertThrows(
                ArithmeticException.class, () -> config.costForLevel(MAX_LEVEL, object.slots()), object.code()
            );
        }
    }

    @ParameterizedTest(name = "Independent slot envelope is a safe upper bound at +{0}")
    @MethodSource("levels")
    void independentSlotBoundsStayInsideCombatAndPowerInputDomains(int level) {
        final var envelope = envelope(level);
        final var largest = envelope(MAX_LEVEL);
        Assertions.assertTrue(envelope.health() <= largest.health());
        Assertions.assertTrue(envelope.attack() <= largest.attack());
        Assertions.assertTrue(envelope.defense() <= largest.defense());
        Assertions.assertTrue(envelope.speed() >= 0 && envelope.speed() < CombatRules.INITIATIVE_THRESHOLD);
        Assertions.assertTrue(envelope.critChance() >= 0 && envelope.critChance() <= 100);
        Assertions.assertTrue(envelope.dodgeChance() >= 0 && envelope.dodgeChance() < 100);
        Assertions.assertTrue(Double.isFinite(envelope.critMultiplier()) && envelope.critMultiplier() >= 1);
        Assertions.assertTrue(envelope.impactContribution() >= 0);
        Assertions.assertTrue(envelope.impactContribution() + ImpactStrength.BASE_VALUE < Integer.MAX_VALUE);
        Assertions.assertEquals(ImpactStrength.MAX_VALUE, Math.min(
            ImpactStrength.MAX_VALUE, ImpactStrength.BASE_VALUE + envelope.impactContribution()
        ));
        assertProbability(envelope.critChance() * 100);
        assertProbability(envelope.dodgeChance() * 100);

        // Every type's defense weight is at most 1.25; combining independent maxima only enlarges the bound.
        for (final var defenseType : DefenseType.values()) {
            for (final var attackType : AttackType.values()) {
                Assertions.assertTrue(CombatRules.defenseWeight(defenseType, attackType) <= 1.25);
            }
        }
        final double minimumDamageMultiplier = CombatRules.DEFENSE_SCALE
            / (CombatRules.DEFENSE_SCALE + 1.25 * envelope.defense());
        final double maximumLivingHealth = envelope.health() / (1 - envelope.dodgeChance() / 100.0)
            / minimumDamageMultiplier;
        final double maximumDamagePerTime = CombatRules.cappedTurnFrequency(Math.toIntExact(envelope.speed()))
            * (1 + envelope.critChance() / 100.0 * (envelope.critMultiplier() - 1)) * envelope.attack();
        final double maximumDisplayedPower = maximumLivingHealth * maximumDamagePerTime
            * PowerDisplayVersion.REFERENCE_V1_SCALE;
        Assertions.assertTrue(Double.isFinite(maximumDisplayedPower));
        Assertions.assertTrue(maximumDisplayedPower >= 0 && maximumDisplayedPower < Integer.MAX_VALUE);

        // These are equipment-only intermediate bounds, including the V2 Berserk bonus but no external effects.
        final long berserkAttack = envelope.attack() + (envelope.attack() * 30 + 79) / 80 + AttackType.values().length;
        final long maximumCritical = (long) Math.ceil(berserkAttack * envelope.critMultiplier());
        final long maximumSkillRaw = (berserkAttack * 30 + 11) / 12;
        final long maximumPacketWithSpread = maximumSkillRaw + maximumSkillRaw / 10;
        final long thornsHealthBasis = Math.multiplyExact(3L, envelope.health());
        final long thornsAttackBasis = Math.multiplyExact(40L, berserkAttack);
        final long maximumHeal = (envelope.health() * 30 + 200) / 400;
        Assertions.assertTrue(maximumCritical + maximumCritical / 10 < Integer.MAX_VALUE);
        Assertions.assertTrue(maximumPacketWithSpread < Integer.MAX_VALUE);
        Assertions.assertTrue(thornsHealthBasis < Integer.MAX_VALUE);
        Assertions.assertTrue(thornsAttackBasis < Integer.MAX_VALUE);
        Assertions.assertTrue(envelope.health() + maximumHeal < Integer.MAX_VALUE);
    }

    @Test
    void frozenEnvelopeAndEveryObtainableSkillPointCountHaveExplicitBounds() {
        final var upper = envelope(MAX_LEVEL);
        Assertions.assertEquals(new Envelope(6_075, 2_160, 1_485, 204, 19, 18, upper.critMultiplier(), 27, 66), upper);
        Assertions.assertEquals(1.75, upper.critMultiplier(), 1e-12);
        final int maximumSkillPoints = ItemRarity.LEGENDARY.skillPoints() * PersonageSlot.values().length;
        Assertions.assertEquals(28, maximumSkillPoints);
        for (int points = 0; points <= maximumSkillPoints; points++) {
            final int coefficient = ScalingSkillMath.multiplierNumerator(points);
            Assertions.assertTrue(coefficient >= 0 && coefficient <= 30);
            final int movementThreshold = ScalingSkillMath.probabilityThresholdFromPercent(
                ScalingSkillMath.multiplier(points).multiply(50)
            );
            Assertions.assertEquals(coefficient * 250, movementThreshold);
            assertProbability(movementThreshold);
            final int effectiveImpact = Math.toIntExact(Math.min(
                ImpactStrength.MAX_VALUE, ImpactStrength.BASE_VALUE + upper.impactContribution()
            ));
            final int maximumDelay = ScalingSkillMath.roundHalfUpToInt(
                NonNegativeRational.of((long) coefficient * effectiveImpact, 5)
            );
            Assertions.assertTrue(maximumDelay >= 0 && maximumDelay <= 600);
        }
        System.out.println("SCALING_V4/V5 equipment numeric domain +0..+75: " + upper
            + "; skill points 0..28; THORNS health basis <=18225; equipment-only maximum SELF_HEAL <=456");
    }

    private static IntStream levels() {
        return IntStream.rangeClosed(0, MAX_LEVEL);
    }

    private static int independentState(int base, int level) {
        return Math.toIntExact((long) base + ((long) base * level + 30) / 60);
    }

    private static Item item(ItemObject object, int level) {
        return new Item(object, Optional.empty(), ItemRarity.COMMON, level);
    }

    private static Envelope envelope(int level) {
        return new Envelope(
            bound(level, Item::health),
            bound(level, item -> item.itemAttacks().stream().mapToLong(part -> part.attack()).sum()),
            bound(level, item -> item.itemDefense().map(defense -> (long) defense.defense()).orElse(0L)),
            bound(level, Item::speed),
            bound(level, Item::critChance),
            bound(level, Item::dodgeChance),
            CombatRules.BASE_CRIT_MULTIPLIER + java.util.Arrays.stream(PersonageSlot.values())
                .mapToDouble(slot -> OBJECTS.stream().filter(object -> object.slots().contains(slot))
                    .mapToDouble(ItemObject::critMultiplier).max().orElseThrow()).sum(),
            bound(level, Item::impact),
            bound(level, Item::baseThreat)
        );
    }

    private static long bound(int level, ToLongFunction<Item> metric) {
        return java.util.Arrays.stream(PersonageSlot.values()).mapToLong(slot -> OBJECTS.stream()
            .filter(object -> object.slots().contains(slot))
            .map(object -> item(object, level))
            .mapToLong(metric)
            .max().orElseThrow()).sum();
    }

    private static void assertProbability(long threshold) {
        Assertions.assertTrue(threshold >= 0 && threshold <= ScalingSkillMath.PROBABILITY_BASIS);
    }

    private record Envelope(
        long health,
        long attack,
        long defense,
        long speed,
        long critChance,
        long dodgeChance,
        double critMultiplier,
        long impactContribution,
        long baseThreat
    ) { }
}
