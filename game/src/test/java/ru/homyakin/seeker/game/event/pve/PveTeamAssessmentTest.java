package ru.homyakin.seeker.game.event.pve;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.BattleRangeAttackInitSnapshot;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;

class PveTeamAssessmentTest {
    @Test
    void exactSingleParticipantUsesHealthDefenseCritAndTurnFrequency() {
        final var personage = prepared(100, 20, 500);
        Mockito.when(personage.defenseReductions()).thenReturn(reductions(0.5));
        Mockito.when(personage.critChance()).thenReturn(25);
        Mockito.when(personage.critMultiplier()).thenReturn(3.0);

        Assertions.assertEquals("NEUTRAL_TEAM_V1", PveTeamAssessment.VERSION);
        Assertions.assertEquals(3000, PveTeamAssessment.assess(List.of(personage)));
    }

    @Test
    void multipliesTeamSumsRatherThanSummingIndividualProducts() {
        final var support = prepared(100, 0, 0);
        Mockito.when(support.defenseReductions()).thenReturn(reductions(0.5));
        final var attacker = prepared(50, 20, 500);

        Assertions.assertEquals(2500, PveTeamAssessment.assess(List.of(support, attacker)));
        Assertions.assertEquals(500, PveTeamAssessment.assess(List.of(attacker)));
        Assertions.assertEquals(2000, PveTeamAssessment.assess(List.of(attacker, attacker)));
    }

    @ParameterizedTest
    @EnumSource(AttackType.class)
    void everyDamageTypeContributesOneQuarterOfNeutralMitigation(AttackType changedType) {
        final var personage = prepared(100, 10, 1000);
        final var reductions = reductions(1);
        reductions.put(changedType, 0.5);
        Mockito.when(personage.defenseReductions()).thenReturn(reductions);

        Assertions.assertEquals(1000 / 0.875, PveTeamAssessment.assess(List.of(personage)));
    }

    @Test
    void takesTheLargestCompleteRangeProfileNotIndependentTypeMaxima() {
        final var personage = prepared(1, 0, 1000);
        Mockito.when(personage.attacksByRange()).thenReturn(List.of(
            profile(1, Map.of(AttackType.SLASH, 70, AttackType.BLUNT, 10)),
            profile(2, Map.of(AttackType.SLASH, 5, AttackType.MAGICAL, 65, AttackType.PIERCE, 5)),
            profile(3, Map.of())
        ));

        Assertions.assertEquals(80, PveTeamAssessment.assess(List.of(personage)));
        Mockito.verify(personage, Mockito.never()).attackAtRange(Mockito.anyInt());
    }

    @Test
    void attackTypeSumsAndFinalProductDoNotOverflowIntegers() {
        final var personage = prepared(Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
        Mockito.when(personage.attacksByRange()).thenReturn(List.of(profile(1, Map.of(
            AttackType.SLASH, Integer.MAX_VALUE, AttackType.BLUNT, Integer.MAX_VALUE,
            AttackType.PIERCE, Integer.MAX_VALUE, AttackType.MAGICAL, Integer.MAX_VALUE
        ))));

        Assertions.assertEquals(4.0 * Integer.MAX_VALUE * Integer.MAX_VALUE,
            PveTeamAssessment.assess(List.of(personage)));
    }

    @ParameterizedTest
    @ValueSource(ints = {1000, 1001, Integer.MAX_VALUE})
    void turnFrequencyIsCappedAtOnePerTick(int speed) {
        Assertions.assertEquals(1000, PveTeamAssessment.assess(List.of(prepared(100, 10, speed))));
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -10, 0})
    void nonPositiveCriticalChanceHasNoCriticalBonus(int chance) {
        final var personage = prepared(100, 10, 1000);
        Mockito.when(personage.critChance()).thenReturn(chance);
        Mockito.when(personage.critMultiplier()).thenReturn(3.0);

        Assertions.assertEquals(1000, PveTeamAssessment.assess(List.of(personage)));
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 101, Integer.MAX_VALUE})
    void criticalChanceAtLeastOneHundredUsesTheMultiplierExactlyOnce(int chance) {
        final var personage = prepared(100, 10, 1000);
        Mockito.when(personage.critChance()).thenReturn(chance);
        Mockito.when(personage.critMultiplier()).thenReturn(3.0);

        Assertions.assertEquals(3000, PveTeamAssessment.assess(List.of(personage)));
    }

    @Test
    void emptyAttackProfileZeroAttackZeroSpeedAndZeroHealthAreValidZeroContributions() {
        final var empty = prepared(100, 10, 1000);
        Mockito.when(empty.attacksByRange()).thenReturn(List.of());
        for (final var personage : List.of(empty, prepared(100, 0, 1000), prepared(100, 10, 0),
            prepared(0, 10, 1000))) {
            Assertions.assertEquals(0, PveTeamAssessment.assess(List.of(personage)));
        }
    }

    @Test
    void allParticipantPermutationsAreBitStableWithoutChangingTheInputList() {
        final var small = prepared(1, 1, 1);
        final var medium = prepared(117, 37, 157);
        final var large = prepared(Integer.MAX_VALUE, Integer.MAX_VALUE, 1000);
        Mockito.when(large.defenseReductions()).thenReturn(reductions(1e-10));
        final var original = List.of(small, medium, large);
        final var expected = PveTeamAssessment.assess(original);

        for (final var order : List.of(
            List.of(small, large, medium), List.of(medium, small, large), List.of(medium, large, small),
            List.of(large, small, medium), List.of(large, medium, small)
        )) {
            Assertions.assertEquals(Double.doubleToLongBits(expected),
                Double.doubleToLongBits(PveTeamAssessment.assess(order)));
        }
        Assertions.assertEquals(List.of(small, medium, large), original);
    }

    @Test
    void realPreparedCopiesIgnorePositionTacticNameDodgeAndEquippedSkills() {
        final var ordinary = realParticipant(Position.FRONT, Optional.of("first"), PersonageEffects.EMPTY,
            Optional.empty(), 0);
        final var expected = PveTeamAssessment.assess(List.of(ordinary));

        for (final var position : Position.values()) {
            final var changed = realParticipant(position, Optional.of("different"), PersonageEffects.EMPTY,
                Optional.of(new Modifier(ActiveEnum.SELF_HEAL)), 100);
            for (final var tactic : TargetingTactic.values()) {
                changed.setTargetingTactic(tactic);
                Assertions.assertEquals(expected, PveTeamAssessment.assess(List.of(changed)));
            }
        }
    }

    @Test
    void preparedHealthAndAttackEffectsAreIncludedExactlyOnceWithoutMutatingCopies() {
        final var ordinary = realParticipant(Position.FRONT, Optional.empty(), PersonageEffects.EMPTY,
            Optional.empty(), 0);
        final var effects = new PersonageEffects(Map.of(
            PersonageEffectType.WORKER_OF_DAY_EFFECT,
            new PersonageEffect(new Effect.Multiplier(50, EffectCharacteristic.HEALTH), null),
            PersonageEffectType.MENU_ITEM_EFFECT,
            new PersonageEffect(new Effect.Multiplier(25, EffectCharacteristic.ATTACK), null)
        ));
        final var changed = realParticipant(Position.FRONT, Optional.empty(), effects, Optional.empty(), 0);
        final var before = changed.attacksByRange();
        final var health = changed.maxHealth();

        Assertions.assertEquals(ordinary.maxHealth() * 1.5, health);
        Assertions.assertEquals(ordinary.attackAtRange(1).get(AttackType.SLASH) * 1.25,
            changed.attackAtRange(1).get(AttackType.SLASH).doubleValue());
        Assertions.assertEquals(PveTeamAssessment.assess(List.of(ordinary)) * 1.5 * 1.25,
            PveTeamAssessment.assess(List.of(changed)), 1e-9);
        Assertions.assertEquals(health, changed.maxHealth());
        Assertions.assertEquals(before, changed.attacksByRange());
        Assertions.assertEquals(effects, effects.activeAt(java.time.LocalDateTime.now()));
    }

    @Test
    void currentDamageDoesNotReplaceMaximumHealthInTheAssessment() {
        final var personage = realParticipant(Position.FRONT, Optional.empty(), PersonageEffects.EMPTY,
            Optional.empty(), 0);
        final var before = PveTeamAssessment.assess(List.of(personage));
        personage.applySkillDamage(AttackType.SLASH, 10, personage, ActiveEnum.BLEEDING, 1);

        Assertions.assertTrue(personage.health() < personage.maxHealth());
        Assertions.assertEquals(before, PveTeamAssessment.assess(List.of(personage)));
    }

    @Test
    void doesNotReadLegacyPowerDisplayedPowerOrEquipmentSnapshots() {
        final var personage = prepared(100, 10, 1000);
        Mockito.when(personage.legacyPower()).thenThrow(new AssertionError("Legacy power must not be consumed"));
        Mockito.when(personage.battlePersonageStats()).thenThrow(new AssertionError("Displayed power must not be consumed"));
        Mockito.when(personage.itemSnapshots()).thenThrow(new AssertionError("Equipment must not be reinterpreted"));
        Mockito.when(personage.skillSnapshots()).thenThrow(new AssertionError("Skills must not receive a price"));

        Assertions.assertEquals(1000, PveTeamAssessment.assess(List.of(personage)));
        Mockito.verify(personage, Mockito.never()).legacyPower();
        Mockito.verify(personage, Mockito.never()).battlePersonageStats();
        Mockito.verify(personage, Mockito.never()).itemSnapshots();
        Mockito.verify(personage, Mockito.never()).skillSnapshots();
    }

    @Test
    void zeroFrequencyOrAttackDoesNotTurnALargeFiniteCriticalFactorIntoNan() {
        for (final var personage : List.of(prepared(100, 0, 1000), prepared(100, Integer.MAX_VALUE, 0))) {
            Mockito.when(personage.critChance()).thenReturn(100);
            Mockito.when(personage.critMultiplier()).thenReturn(Double.MAX_VALUE);
            Assertions.assertEquals(0, PveTeamAssessment.assess(List.of(personage)));
        }
    }

    @Test
    void zeroCriticalMultiplierAtCertainCritChanceIsAValidZeroDamageFactor() {
        final var personage = prepared(100, 10, 1000);
        Mockito.when(personage.critChance()).thenReturn(100);
        Mockito.when(personage.critMultiplier()).thenReturn(0.0);

        Assertions.assertEquals(0, PveTeamAssessment.assess(List.of(personage)));
    }

    @Test
    void rejectsNullEmptyTeamsAndNullParticipants() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> PveTeamAssessment.assess(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> PveTeamAssessment.assess(List.of()));
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> PveTeamAssessment.assess(Arrays.asList(prepared(1, 1, 1), null)));
    }

    @Test
    void rejectsNegativePreparedHealthSpeedDefenseAndAttack() {
        final var negativeHealth = prepared(-1, 1, 1);
        final var negativeSpeed = prepared(1, 1, -1);
        final var negativeAttack = prepared(1, -1, 1);
        final var negativeDefense = prepared(1, 1, 1);
        Mockito.when(negativeDefense.defenses()).thenReturn(Map.of(DefenseType.PLATE, -1));

        for (final var personage : List.of(negativeHealth, negativeSpeed, negativeAttack, negativeDefense)) {
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> PveTeamAssessment.assess(List.of(personage)));
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
    void rejectsInvalidCriticalMultipliers(double multiplier) {
        final var personage = prepared(1, 1, 1);
        Mockito.when(personage.critMultiplier()).thenReturn(multiplier);

        Assertions.assertThrows(IllegalArgumentException.class,
            () -> PveTeamAssessment.assess(List.of(personage)));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
    void rejectsNonPositiveOrNonFiniteDamageTakenMultipliers(double multiplier) {
        final var personage = prepared(1, 1, 1);
        final var reductions = reductions(1);
        reductions.put(AttackType.MAGICAL, multiplier);
        Mockito.when(personage.defenseReductions()).thenReturn(reductions);

        Assertions.assertThrows(IllegalArgumentException.class,
            () -> PveTeamAssessment.assess(List.of(personage)));
    }

    @Test
    void rejectsMissingAndNullPreparedSnapshotFields() {
        final var missingReduction = prepared(1, 1, 1);
        Mockito.when(missingReduction.defenseReductions()).thenReturn(Map.of());
        final var nullReductions = prepared(1, 1, 1);
        Mockito.when(nullReductions.defenseReductions()).thenReturn(null);
        final var nullDefenses = prepared(1, 1, 1);
        Mockito.when(nullDefenses.defenses()).thenReturn(null);
        final var nullAttackProfiles = prepared(1, 1, 1);
        Mockito.when(nullAttackProfiles.attacksByRange()).thenReturn(null);
        final var nullProfile = prepared(1, 1, 1);
        Mockito.when(nullProfile.attacksByRange()).thenReturn(Collections.singletonList(null));
        final var nullAttackMap = prepared(1, 1, 1);
        Mockito.when(nullAttackMap.attacksByRange()).thenReturn(List.of(profile(1, null)));
        final var invalidRange = prepared(1, 1, 1);
        Mockito.when(invalidRange.attacksByRange()).thenReturn(List.of(profile(0, Map.of())));
        final var nullAttackValue = prepared(1, 1, 1);
        final var attacks = new HashMap<AttackType, Integer>();
        attacks.put(AttackType.SLASH, null);
        Mockito.when(nullAttackValue.attacksByRange()).thenReturn(List.of(profile(1, attacks)));
        final var nullDefenseValue = prepared(1, 1, 1);
        final var defenses = new HashMap<DefenseType, Integer>();
        defenses.put(DefenseType.PLATE, null);
        Mockito.when(nullDefenseValue.defenses()).thenReturn(defenses);

        for (final var personage : List.of(missingReduction, nullReductions, nullDefenses, nullAttackProfiles,
            nullProfile, nullAttackMap, invalidRange, nullAttackValue, nullDefenseValue)) {
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> PveTeamAssessment.assess(List.of(personage)));
        }
    }

    @Test
    void rejectsNonFiniteIntermediateAndFinalCalculationsInsteadOfReturningInfinityOrNan() {
        final var healthOverflow = prepared(Integer.MAX_VALUE, 1, 1);
        Mockito.when(healthOverflow.defenseReductions()).thenReturn(reductions(Double.MIN_VALUE));
        final var mitigationOverflow = prepared(1, 1, 1);
        Mockito.when(mitigationOverflow.defenseReductions()).thenReturn(reductions(Double.MAX_VALUE));
        final var attackOverflow = prepared(1, Integer.MAX_VALUE, 1000);
        Mockito.when(attackOverflow.critChance()).thenReturn(100);
        Mockito.when(attackOverflow.critMultiplier()).thenReturn(Double.MAX_VALUE);
        final var productOverflow = prepared(1, 10, 1000);
        Mockito.when(productOverflow.defenseReductions()).thenReturn(reductions(1e-308));

        for (final var personage : List.of(healthOverflow, mitigationOverflow, attackOverflow, productOverflow)) {
            Assertions.assertThrows(IllegalArgumentException.class,
                () -> PveTeamAssessment.assess(List.of(personage)));
        }
    }

    @Test
    void increasingAnIncludedCharacteristicDoesNotLowerTheAssessmentForNormalCriticalMultipliers() {
        final var base = prepared(100, 100, 100);
        final var expected = PveTeamAssessment.assess(List.of(base));
        final var moreHealth = prepared(101, 100, 100);
        final var moreAttack = prepared(100, 101, 100);
        final var moreSpeed = prepared(100, 100, 101);
        final var moreDefense = prepared(100, 100, 100);
        Mockito.when(moreDefense.defenseReductions()).thenReturn(reductions(0.9));
        final var moreCrit = prepared(100, 100, 100);
        Mockito.when(moreCrit.critChance()).thenReturn(1);
        Mockito.when(moreCrit.critMultiplier()).thenReturn(2.0);

        for (final var personage : List.of(moreHealth, moreAttack, moreSpeed, moreDefense, moreCrit)) {
            Assertions.assertTrue(PveTeamAssessment.assess(List.of(personage)) >= expected);
        }
    }

    private static BattlePersonage prepared(int health, int attack, int speed) {
        final var personage = Mockito.mock(BattlePersonage.class);
        Mockito.when(personage.maxHealth()).thenReturn(health);
        Mockito.when(personage.initiative()).thenReturn(speed);
        Mockito.when(personage.critMultiplier()).thenReturn(1.0);
        Mockito.when(personage.defenses()).thenReturn(Map.of());
        Mockito.when(personage.defenseReductions()).thenReturn(reductions(1));
        Mockito.when(personage.attacksByRange()).thenReturn(List.of(profile(1, Map.of(AttackType.SLASH, attack))));
        return personage;
    }

    private static EnumMap<AttackType, Double> reductions(double multiplier) {
        final var reductions = new EnumMap<AttackType, Double>(AttackType.class);
        for (final var type : AttackType.values()) {
            reductions.put(type, multiplier);
        }
        return reductions;
    }

    private static BattleRangeAttackInitSnapshot profile(int range, Map<AttackType, Integer> attack) {
        return new BattleRangeAttackInitSnapshot(range, attack);
    }

    private static BattlePersonage realParticipant(
        Position position, Optional<String> name, PersonageEffects effects, Optional<Modifier> modifier, int dodge
    ) {
        final var object = new ItemObject(
            null, Set.of(PersonageSlot.MAIN_HAND), Optional.of(new ItemAttack(AttackType.SLASH, 1, 100)),
            Optional.of(new ItemDefense(DefenseType.PLATE, 100)), 1000, 10, dodge, 0.3, 200, 10, null
        );
        return BattlePersonage.forCombat(List.of(new Item(object, modifier, ItemRarity.LEGENDARY)), position,
            effects, name, SkillFormulaVersion.SCALING_SKILLS_V3);
    }
}
