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
 * Maggeese Flock raid: a bossless swarm of enchanted geese.
 *
 * <p>Two unit roles, all wearing CLOTH "plumage" armour:
 * <ul>
 *   <li><b>Beak Chargers</b> (front) — aggressive melee geese with SLASH attacks
 *       and {@link ActiveEnum#DOUBLE_ATTACK}, pecking twice per turn.</li>
 *   <li><b>Mageese</b> (mid) — arcane geese hurling MAGICAL bolts with
 *       {@link ActiveEnum#HIT_AND_RUN}, using the versioned attack-and-retreat rules.</li>
 * </ul>
 *
 * <p>Counter-pick hint: SLASH weapons exploit CLOTH's lowest resistance (×0.75
 * effective defence); PLATE armour best resists the Beak Chargers' SLASH damage.
 */
public class MaggeeseFlockGenerator implements RaidBattlePersonageGenerator {

    // --- beak charger (front) base stats ---
    private static final int BASE_CHARGER_ATTACK = 95;
    private static final int BASE_CHARGER_DEFENSE = 70;
    private static final int BASE_CHARGER_HEALTH = 320;
    private static final int BASE_CHARGER_CRIT_CHANCE = 6;
    private static final int BASE_CHARGER_DODGE_CHANCE = 8;
    private static final double BASE_CHARGER_CRIT_MULTI = 0.4;
    private static final int BASE_CHARGER_SPEED = 280;
    private static final int BASE_CHARGER_THREAT = 30;

    // --- mageese (mid) base stats ---
    private static final int BASE_MAGEESE_ATTACK = 120;
    private static final int BASE_MAGEESE_DEFENSE = 50;
    private static final int BASE_MAGEESE_HEALTH = 270;
    private static final int BASE_MAGEESE_CRIT_CHANCE = 10;
    private static final int BASE_MAGEESE_DODGE_CHANCE = 8;
    private static final double BASE_MAGEESE_CRIT_MULTI = 0.6;
    private static final int BASE_MAGEESE_SPEED = 220;
    private static final int BASE_MAGEESE_THREAT = 20;

    private static final double GROUP_SCALE_COEF = 0.05;

    private static final double LEGACY_POWER_CALIBRATION_CORRECTION = 0.75;
    private static final double EQUIPMENT_POWER_CALIBRATION_CORRECTION = 0.725;

    @Override
    public GeneratedPveEnemies generateCalibrated(
        List<BattlePersonage> personages,
        double powerBonus,
        SkillFormulaVersion formulaVersion
    ) {
        RaidBattlePersonageGenerator.validateInputs(personages, powerBonus, formulaVersion);
        final int flockSize = Math.max(2, personages.size());
        if (formulaVersion == SkillFormulaVersion.SCALING_SKILLS_V3) {
            return generateEquipmentCalibrated(personages, powerBonus, flockSize, formulaVersion);
        }
        final double groupSizeScaling;
        if (flockSize <= 10) {
            groupSizeScaling = 1.0 + MathUtils.log(2.2, flockSize) * GROUP_SCALE_COEF;
        } else {
            groupSizeScaling = 1.0 + MathUtils.log(4, flockSize) * GROUP_SCALE_COEF;
        }
        final var targetPower = personages.stream()
            .mapToDouble(BattlePersonage::legacyPower)
            .sum() * powerBonus * groupSizeScaling * LEGACY_POWER_CALIBRATION_CORRECTION;
        final var calibration = PveCharacteristicScaling.search(
            targetPower,
            PveCharacteristicScaling.maxMultiplier(
                BASE_CHARGER_ATTACK, BASE_CHARGER_DEFENSE, BASE_CHARGER_HEALTH,
                BASE_MAGEESE_ATTACK, BASE_MAGEESE_DEFENSE, BASE_MAGEESE_HEALTH
            ),
            multiplier -> totalRaidPower(flockSize, multiplier, formulaVersion)
        );
        final var multiplier = calibration.multiplier();

        final int frontCount = flockSize / 2;
        final int midCount = flockSize - frontCount;

        final var flock = new ArrayList<BattlePersonage>();
        for (int i = 0; i < frontCount; i++) {
            flock.add(charger(multiplier, formulaVersion));
        }
        for (int i = 0; i < midCount; i++) {
            flock.add(mageese(multiplier, formulaVersion));
        }
        return new GeneratedPveEnemies(flock, calibration);
    }

    private GeneratedPveEnemies generateEquipmentCalibrated(
        List<BattlePersonage> personages,
        double powerBonus,
        int flockSize,
        SkillFormulaVersion formulaVersion
    ) {
        final double groupSizeScaling;
        if (flockSize <= 10) {
            groupSizeScaling = 1.0 + MathUtils.log(2.2, flockSize) * GROUP_SCALE_COEF;
        } else {
            final double boundary = 1.0 + MathUtils.log(2.2, 10) * GROUP_SCALE_COEF;
            groupSizeScaling = boundary + MathUtils.log(4, flockSize / 10.0) * GROUP_SCALE_COEF;
        }
        final var participantAssessment = PveEquipmentPowerSum.sum(personages);
        final var target = participantAssessment * powerBonus * groupSizeScaling
            * EQUIPMENT_POWER_CALIBRATION_CORRECTION;
        final var calibration = PveCharacteristicScaling.search(
            target,
            PveCharacteristicScaling.maxMultiplier(
                BASE_CHARGER_ATTACK, BASE_CHARGER_DEFENSE, BASE_CHARGER_HEALTH,
                BASE_MAGEESE_ATTACK, BASE_MAGEESE_DEFENSE, BASE_MAGEESE_HEALTH
            ),
            multiplier -> RandomUtils.withSeed(0, () -> PveEquipmentPowerSum.sum(
                equipmentFlock(flockSize, multiplier, formulaVersion)))
        );
        final var diagnostics = new PveScalingDiagnostics(PveEquipmentPowerSum.VERSION, participantAssessment, Map.of(
            "enemyCount", (double) flockSize,
            "frontCount", (double) (flockSize / 2),
            "midCount", (double) (flockSize - flockSize / 2),
            "groupSizeScaling", groupSizeScaling,
            "powerBonus", powerBonus,
            "encounterCorrection", EQUIPMENT_POWER_CALIBRATION_CORRECTION
        ));
        return new GeneratedPveEnemies(
            equipmentFlock(flockSize, calibration.multiplier(), formulaVersion), calibration, Optional.of(diagnostics)
        );
    }

    private List<BattlePersonage> equipmentFlock(int flockSize, double multiplier, SkillFormulaVersion version) {
        final var flock = new ArrayList<BattlePersonage>(flockSize);
        final int frontCount = flockSize / 2;
        for (int index = 0; index < frontCount; index++) {
            flock.add(charger(multiplier, version));
        }
        for (int index = frontCount; index < flockSize; index++) {
            flock.add(mageese(multiplier, version));
        }
        return flock;
    }

    // -------------------------------------------------------------------------
    // Unit factories
    // -------------------------------------------------------------------------

    private BattlePersonage charger(double m, SkillFormulaVersion formulaVersion) {
        return BattlePersonage.forCombat(
            List.of(
                new Item(
                    new ItemObject(
                        null,
                        Set.of(PersonageSlot.MAIN_HAND),
                        Optional.of(new ItemAttack(AttackType.SLASH, 1, multiply(BASE_CHARGER_ATTACK, m))),
                        Optional.of(new ItemDefense(DefenseType.CLOTH, multiply(BASE_CHARGER_DEFENSE, m))),
                        multiply(BASE_CHARGER_HEALTH, m),
                        BASE_CHARGER_CRIT_CHANCE, BASE_CHARGER_DODGE_CHANCE, BASE_CHARGER_CRIT_MULTI,
                        BASE_CHARGER_SPEED, BASE_CHARGER_THREAT, null
                    ),
                    Optional.of(new Modifier(ActiveEnum.DOUBLE_ATTACK)),
                    ItemRarity.LEGENDARY
                )
            ),
            Position.FRONT,
            PersonageEffects.EMPTY,
            Optional.empty(),
            formulaVersion
        );
    }

    private BattlePersonage mageese(double m, SkillFormulaVersion formulaVersion) {
        return BattlePersonage.forCombat(
            List.of(
                new Item(
                    new ItemObject(
                        null,
                        Set.of(PersonageSlot.MAIN_HAND),
                        Optional.of(new ItemAttack(AttackType.MAGICAL, 1,
                            formulaVersion == SkillFormulaVersion.SCALING_SKILLS_V3 ? 2 : 1,
                            multiply(BASE_MAGEESE_ATTACK, m))),
                        Optional.of(new ItemDefense(DefenseType.CLOTH, multiply(BASE_MAGEESE_DEFENSE, m))),
                        multiply(BASE_MAGEESE_HEALTH, m),
                        BASE_MAGEESE_CRIT_CHANCE, BASE_MAGEESE_DODGE_CHANCE, BASE_MAGEESE_CRIT_MULTI,
                        BASE_MAGEESE_SPEED, BASE_MAGEESE_THREAT, null
                    ),
                    Optional.of(new Modifier(ActiveEnum.HIT_AND_RUN)),
                    ItemRarity.LEGENDARY
                )
            ),
            Position.MID,
            PersonageEffects.EMPTY,
            Optional.empty(),
            formulaVersion
        );
    }

    // -------------------------------------------------------------------------
    // The trial factories use the same skill formulas as the final enemies.
    // -------------------------------------------------------------------------

    private double totalRaidPower(int flockSize, double m, SkillFormulaVersion formulaVersion) {
        final int frontCount = flockSize / 2;
        final int midCount = flockSize - frontCount;
        return charger(m, formulaVersion).legacyPower() * frontCount
            + mageese(m, formulaVersion).legacyPower() * midCount;
    }

    private int multiply(int value, double multiplier) {
        return PveCharacteristicScaling.multiply(value, multiplier);
    }
}
