package ru.homyakin.seeker.game.event.raid.generator;

import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.event.pve.GeneratedPveEnemies;
import ru.homyakin.seeker.game.event.pve.PveCharacteristicScaling;
import ru.homyakin.seeker.game.event.pve.PveEquipmentPowerSum;
import ru.homyakin.seeker.game.event.pve.PveScalingDiagnostics;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Zombie Horde raid: slow, heavily PLATE-armoured undead.
 * Each zombie carries a single randomly chosen BLUNT or SLASH attack —
 * each attack remains fixed throughout matching and the final V3 encounter.
 * The personal equipment assessment is neutral to the attacker's own type;
 * this is not a prediction of the resulting encounter's difficulty.
 * Regular zombies carry {@link ActiveEnum#THORNS}; the boss carries
 * {@link ActiveEnum#SELF_HEAL}.
 */
public class ZombieHordeGenerator implements RaidBattlePersonageGenerator {

    // --- regular zombie base stats ---
    private static final int BASE_ATTACK = 80;
    private static final int BASE_DEFENSE = 200;
    private static final int BASE_HEALTH = 500;
    private static final int BASE_CRIT_CHANCE = 3;
    private static final int BASE_DODGE_CHANCE = 2;
    private static final double BASE_CRIT_MULTI = 0.3;
    private static final int BASE_SPEED = 200;
    private static final int BASE_THREAT = 25;

    // --- boss zombie base stats ---
    private static final int BASE_BOSS_ATTACK = 180;
    private static final int BASE_BOSS_DEFENSE = 350;
    private static final int BASE_BOSS_HEALTH = 2000;
    private static final int BASE_BOSS_CRIT_CHANCE = 8;
    private static final int BASE_BOSS_DODGE_CHANCE = 3;
    private static final double BASE_BOSS_CRIT_MULTI = 0.5;
    private static final int BASE_BOSS_SPEED = 150;
    private static final int BASE_BOSS_THREAT = 50;

    /** Each extra group member forces the raid to scale up by this log factor. */
    private static final double GROUP_SCALE_COEF = 0.05;
    // Step 17.2: a single encounter correction; it does not depend on the participant's build or level.
    private static final double EQUIPMENT_POWER_CALIBRATION_CORRECTION = 0.76;

    @Override
    public GeneratedPveEnemies generateCalibrated(
        List<BattlePersonage> personages,
        double powerBonus,
        SkillFormulaVersion formulaVersion
    ) {
        RaidBattlePersonageGenerator.validateInputs(personages, powerBonus, formulaVersion);
        final int zombieCount = personages.size();
        if (formulaVersion == SkillFormulaVersion.SCALING_SKILLS_V3) {
            return generateEquipmentCalibrated(personages, powerBonus, zombieCount, formulaVersion);
        }
        final double groupSizeScaling;
        if (personages.size() <= 10) {
            groupSizeScaling = 1.0 + MathUtils.log(2.2, zombieCount) * GROUP_SCALE_COEF;
        } else if (personages.size() <= 13) {
            groupSizeScaling = 1.0 + MathUtils.log(4, zombieCount) * GROUP_SCALE_COEF;
        } else {
            groupSizeScaling = 1.0 + MathUtils.log(8, zombieCount) * GROUP_SCALE_COEF;
        }
        final var targetPower = personages.stream()
            .mapToDouble(BattlePersonage::legacyPower)
            .sum() * powerBonus * groupSizeScaling;
        final var calibration = PveCharacteristicScaling.search(
            targetPower,
            PveCharacteristicScaling.maxMultiplier(
                BASE_ATTACK, BASE_DEFENSE, BASE_HEALTH,
                BASE_BOSS_ATTACK, BASE_BOSS_DEFENSE, BASE_BOSS_HEALTH
            ),
            multiplier -> totalRaidPower(zombieCount, multiplier, formulaVersion)
        );
        final var multiplier = calibration.multiplier();

        final var horde = new ArrayList<BattlePersonage>();
        horde.add(boss(multiplier, randomAttack(), formulaVersion));
        for (int i = 0; i < zombieCount; i++) {
            horde.add(zombie(multiplier, randomAttack(), formulaVersion));
        }
        return new GeneratedPveEnemies(horde, calibration);
    }

    private GeneratedPveEnemies generateEquipmentCalibrated(
        List<BattlePersonage> personages,
        double powerBonus,
        int count,
        SkillFormulaVersion version
    ) {
        final var participantAssessment = PveEquipmentPowerSum.sum(personages);
        final var groupScaling = equipmentGroupScaling(count);
        final var target = participantAssessment * powerBonus * groupScaling
            * EQUIPMENT_POWER_CALIBRATION_CORRECTION;
        final var attacks = new ArrayList<AttackType>();
        for (int index = 0; index <= count; ++index) {
            attacks.add(randomAttack());
        }
        final var attackTypes = List.copyOf(attacks);
        final var calibration = PveCharacteristicScaling.search(
            target,
            PveCharacteristicScaling.maxMultiplier(
                BASE_ATTACK, BASE_DEFENSE, BASE_HEALTH,
                BASE_BOSS_ATTACK, BASE_BOSS_DEFENSE, BASE_BOSS_HEALTH
            ),
            multiplier -> RandomUtils.withSeed(0, () -> PveEquipmentPowerSum.sum(
                equipmentHorde(attackTypes, multiplier, version)))
        );
        final var bluntCount = attackTypes.stream().filter(type -> type == AttackType.BLUNT).count();
        final var diagnostics = new PveScalingDiagnostics(PveEquipmentPowerSum.VERSION, participantAssessment, Map.of(
            "enemyCount", (double) attackTypes.size(),
            "zombieCount", (double) count,
            "bossCount", 1.0,
            "bluntCount", (double) bluntCount,
            "slashCount", (double) attackTypes.size() - bluntCount,
            "groupSizeScaling", groupScaling,
            "powerBonus", powerBonus,
            "encounterCorrection", EQUIPMENT_POWER_CALIBRATION_CORRECTION
        ));
        return new GeneratedPveEnemies(
            equipmentHorde(attackTypes, calibration.multiplier(), version), calibration, Optional.of(diagnostics)
        );
    }

    private static double equipmentGroupScaling(int count) {
        if (count <= 10) {
            return 1.0 + MathUtils.log(2.2, count) * GROUP_SCALE_COEF;
        }
        final var atTen = 1.0 + MathUtils.log(2.2, 10) * GROUP_SCALE_COEF;
        if (count <= 13) {
            return atTen + MathUtils.log(4, count / 10.0) * GROUP_SCALE_COEF;
        }
        final var atThirteen = atTen + MathUtils.log(4, 13 / 10.0) * GROUP_SCALE_COEF;
        return atThirteen + MathUtils.log(8, count / 13.0) * GROUP_SCALE_COEF;
    }

    private List<BattlePersonage> equipmentHorde(List<AttackType> attacks, double multiplier, SkillFormulaVersion version) {
        final var horde = new ArrayList<BattlePersonage>(attacks.size());
        horde.add(boss(multiplier, attacks.getFirst(), version));
        for (int index = 1; index < attacks.size(); ++index) {
            horde.add(zombie(multiplier, attacks.get(index), version));
        }
        return horde;
    }

    // -------------------------------------------------------------------------
    // Unit factories
    // -------------------------------------------------------------------------

    private BattlePersonage zombie(double m, AttackType attackType, SkillFormulaVersion formulaVersion) {
        return BattlePersonage.forCombat(
            List.of(
                new Item(
                    new ItemObject(
                        null,
                        Set.of(PersonageSlot.MAIN_HAND),
                        Optional.of(new ItemAttack(attackType, 1, multiply(BASE_ATTACK, m))),
                        Optional.of(new ItemDefense(DefenseType.PLATE, multiply(BASE_DEFENSE, m))),
                        multiply(BASE_HEALTH, m),
                        BASE_CRIT_CHANCE, BASE_DODGE_CHANCE, BASE_CRIT_MULTI,
                        BASE_SPEED, BASE_THREAT, null
                    ),
                    Optional.of(new Modifier(ActiveEnum.THORNS)),
                    ItemRarity.LEGENDARY
                )
            ),
            Position.FRONT,
            PersonageEffects.EMPTY,
            Optional.empty(),
            formulaVersion
        );
    }

    private BattlePersonage boss(double m, AttackType attackType, SkillFormulaVersion formulaVersion) {
        return BattlePersonage.forCombat(
            List.of(
                new Item(
                    new ItemObject(
                        null,
                        Set.of(PersonageSlot.MAIN_HAND),
                        Optional.of(new ItemAttack(attackType, 1, multiply(BASE_BOSS_ATTACK, m))),
                        Optional.of(new ItemDefense(DefenseType.PLATE, multiply(BASE_BOSS_DEFENSE, m))),
                        multiply(BASE_BOSS_HEALTH, m),
                        BASE_BOSS_CRIT_CHANCE, BASE_BOSS_DODGE_CHANCE, BASE_BOSS_CRIT_MULTI,
                        BASE_BOSS_SPEED, BASE_BOSS_THREAT, null
                    ),
                    Optional.of(new Modifier(ActiveEnum.SELF_HEAL)),
                    ItemRarity.LEGENDARY
                )
            ),
            Position.FRONT,
            PersonageEffects.EMPTY,
            Optional.empty(),
            formulaVersion
        );
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Returns BLUNT or SLASH with equal probability. */
    private static AttackType randomAttack() {
        return RandomUtils.processChance(50) ? AttackType.BLUNT : AttackType.SLASH;
    }

    // -------------------------------------------------------------------------
    // Power calibration (binary search — attack type fixed to SLASH for
    // determinism; power() is independent of the unit's own attack type)
    // -------------------------------------------------------------------------

    private double totalRaidPower(int zombieCount, double m, SkillFormulaVersion formulaVersion) {
        return boss(m, AttackType.SLASH, formulaVersion).legacyPower()
            + zombie(m, AttackType.SLASH, formulaVersion).legacyPower() * zombieCount;
    }

    private int multiply(int value, double multiplier) {
        return PveCharacteristicScaling.multiply(value, multiplier);
    }
}
