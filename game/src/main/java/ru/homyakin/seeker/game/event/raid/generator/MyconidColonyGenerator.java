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
 * Myconid Colony raid: a bossless fungal horde wielding MAGICAL attacks behind
 * ARCANE defences.
 *
 * <p>The colony always fields at least two units and splits them between roles:
 * <ul>
 *   <li><b>Spore Guardians</b> (front) — tanky melee fighters with
 *       {@link ActiveEnum#KNOCKBACK} that shove attackers away.</li>
 *   <li><b>Spore Slingers</b> (back) — fragile but hard-hitting ranged casters
 *       with {@link ActiveEnum#PRECISE_STRIKE}.</li>
 * </ul>
 *
 * <p>Counter-pick hint: ARCANE defence resists the colony's MAGICAL attacks
 * best (×1.25 mitigation), while PIERCE weapons cut through ARCANE shields
 * most efficiently (×0.75 effective defence).
 */
public class MyconidColonyGenerator implements RaidBattlePersonageGenerator {

    // --- spore guardian (front) base stats ---
    private static final int BASE_GUARDIAN_ATTACK = 70;
    private static final int BASE_GUARDIAN_DEFENSE = 160;
    private static final int BASE_GUARDIAN_HEALTH = 420;
    private static final int BASE_GUARDIAN_CRIT_CHANCE = 4;
    private static final int BASE_GUARDIAN_DODGE_CHANCE = 3;
    private static final double BASE_GUARDIAN_CRIT_MULTI = 0.3;
    private static final int BASE_GUARDIAN_SPEED = 190;
    private static final int BASE_GUARDIAN_THREAT = 35;

    // --- spore slinger (back) base stats ---
    private static final int BASE_SLINGER_ATTACK = 130;
    private static final int BASE_SLINGER_DEFENSE = 70;
    private static final int BASE_SLINGER_HEALTH = 280;
    private static final int BASE_SLINGER_CRIT_CHANCE = 8;
    private static final int BASE_SLINGER_DODGE_CHANCE = 5;
    private static final double BASE_SLINGER_CRIT_MULTI = 0.5;
    private static final int BASE_SLINGER_SPEED = 160;
    private static final int BASE_SLINGER_THREAT = 15;

    private static final double GROUP_SCALE_COEF = 0.05;

    // Historical formulas retain their original generation until the explicit V3 release.
    private static final double LEGACY_POWER_CALIBRATION_CORRECTION = 0.77;
    // Step 17.2 keeps the encounter correction separate from historical formula versions.
    private static final double EQUIPMENT_POWER_CALIBRATION_CORRECTION = 0.82;

    @Override
    public GeneratedPveEnemies generateCalibrated(
        List<BattlePersonage> personages,
        double powerBonus,
        SkillFormulaVersion formulaVersion
    ) {
        RaidBattlePersonageGenerator.validateInputs(personages, powerBonus, formulaVersion);
        final int myconidCount = Math.max(2, personages.size());
        if (formulaVersion == SkillFormulaVersion.SCALING_SKILLS_V3) {
            return generateEquipmentCalibrated(personages, powerBonus, myconidCount, formulaVersion);
        }
        final double groupSizeScaling;
        if (myconidCount <= 7) {
            groupSizeScaling = 1.0 + MathUtils.log(2.2, myconidCount) * GROUP_SCALE_COEF;
        } else {
            groupSizeScaling = 1.0 + MathUtils.log(4, myconidCount) * GROUP_SCALE_COEF;
        }
        final var targetPower = personages.stream()
            .mapToDouble(BattlePersonage::legacyPower)
            .sum() * powerBonus * groupSizeScaling * LEGACY_POWER_CALIBRATION_CORRECTION;
        final var calibration = PveCharacteristicScaling.search(
            targetPower,
            PveCharacteristicScaling.maxMultiplier(
                BASE_GUARDIAN_ATTACK, BASE_GUARDIAN_DEFENSE, BASE_GUARDIAN_HEALTH,
                BASE_SLINGER_ATTACK, BASE_SLINGER_DEFENSE, BASE_SLINGER_HEALTH
            ),
            multiplier -> totalRaidPower(myconidCount, multiplier, formulaVersion)
        );
        final var multiplier = calibration.multiplier();

        final int frontCount = myconidCount / 2;
        final int backCount = myconidCount - frontCount;

        final var colony = new ArrayList<BattlePersonage>();
        for (int i = 0; i < frontCount; i++) {
            colony.add(guardian(multiplier, formulaVersion));
        }
        for (int i = 0; i < backCount; i++) {
            colony.add(slinger(multiplier, formulaVersion));
        }
        return new GeneratedPveEnemies(colony, calibration);
    }

    private GeneratedPveEnemies generateEquipmentCalibrated(
        List<BattlePersonage> personages,
        double powerBonus,
        int count,
        SkillFormulaVersion formulaVersion
    ) {
        final var formation = new Formation(count);
        final var groupSizeScaling = 1.0 + MathUtils.log(4, count) * GROUP_SCALE_COEF;
        final var participantAssessment = PveEquipmentPowerSum.sum(personages);
        final var target = participantAssessment * powerBonus * groupSizeScaling
            * EQUIPMENT_POWER_CALIBRATION_CORRECTION;
        final var calibration = PveCharacteristicScaling.search(
            target,
            PveCharacteristicScaling.maxMultiplier(
                BASE_GUARDIAN_ATTACK, BASE_GUARDIAN_DEFENSE, BASE_GUARDIAN_HEALTH,
                BASE_SLINGER_ATTACK, BASE_SLINGER_DEFENSE, BASE_SLINGER_HEALTH
            ),
            // UUIDs and initial initiative are irrelevant to power. Preserve the final random stream.
            multiplier -> RandomUtils.withSeed(0, () -> PveEquipmentPowerSum.sum(
                equipmentColony(formation, multiplier, formulaVersion)))
        );
        final var diagnostics = new PveScalingDiagnostics(PveEquipmentPowerSum.VERSION, participantAssessment, Map.of(
            "enemyCount", (double) formation.count(),
            "frontCount", (double) formation.frontCount(),
            "backCount", (double) formation.backCount(),
            "groupSizeScaling", groupSizeScaling,
            "powerBonus", powerBonus,
            "encounterCorrection", EQUIPMENT_POWER_CALIBRATION_CORRECTION
        ));
        return new GeneratedPveEnemies(
            equipmentColony(formation, calibration.multiplier(), formulaVersion), calibration, Optional.of(diagnostics)
        );
    }

    private List<BattlePersonage> equipmentColony(Formation formation, double multiplier, SkillFormulaVersion version) {
        final var colony = new ArrayList<BattlePersonage>(formation.count());
        for (int i = 0; i < formation.frontCount(); i++) {
            colony.add(guardian(multiplier, version));
        }
        for (int i = 0; i < formation.backCount(); i++) {
            colony.add(slinger(multiplier, version));
        }
        return colony;
    }

    // -------------------------------------------------------------------------
    // Unit factories
    // -------------------------------------------------------------------------

    private BattlePersonage guardian(double m, SkillFormulaVersion formulaVersion) {
        return BattlePersonage.forCombat(
            List.of(
                new Item(
                    new ItemObject(
                        null,
                        Set.of(PersonageSlot.MAIN_HAND),
                        Optional.of(new ItemAttack(AttackType.MAGICAL, 1,
                            multiply(BASE_GUARDIAN_ATTACK, m))),
                        Optional.of(new ItemDefense(DefenseType.ARCANE, multiply(BASE_GUARDIAN_DEFENSE, m))),
                        multiply(BASE_GUARDIAN_HEALTH, m),
                        BASE_GUARDIAN_CRIT_CHANCE, BASE_GUARDIAN_DODGE_CHANCE, BASE_GUARDIAN_CRIT_MULTI,
                        BASE_GUARDIAN_SPEED, BASE_GUARDIAN_THREAT, null
                    ),
                    Optional.of(new Modifier(ActiveEnum.KNOCKBACK)),
                    ItemRarity.LEGENDARY
                )
            ),
            Position.FRONT,
            PersonageEffects.EMPTY,
            Optional.empty(),
            formulaVersion
        );
    }

    private BattlePersonage slinger(double m, SkillFormulaVersion formulaVersion) {
        return BattlePersonage.forCombat(
            List.of(
                new Item(
                    new ItemObject(
                        null,
                        Set.of(PersonageSlot.MAIN_HAND),
                        Optional.of(new ItemAttack(
                            AttackType.MAGICAL, 1,
                            formulaVersion == SkillFormulaVersion.SCALING_SKILLS_V3 ? 3 : 1,
                            multiply(BASE_SLINGER_ATTACK, m)
                        )),
                        Optional.of(new ItemDefense(DefenseType.ARCANE, multiply(BASE_SLINGER_DEFENSE, m))),
                        multiply(BASE_SLINGER_HEALTH, m),
                        BASE_SLINGER_CRIT_CHANCE, BASE_SLINGER_DODGE_CHANCE, BASE_SLINGER_CRIT_MULTI,
                        BASE_SLINGER_SPEED, BASE_SLINGER_THREAT, null
                    ),
                    Optional.of(new Modifier(ActiveEnum.PRECISE_STRIKE)),
                    ItemRarity.LEGENDARY
                )
            ),
            Position.BACK,
            PersonageEffects.EMPTY,
            Optional.empty(),
            formulaVersion
        );
    }

    // -------------------------------------------------------------------------
    // The trial factories use the same skill formulas as the final enemies.
    // -------------------------------------------------------------------------

    private double totalRaidPower(int myconidCount, double m, SkillFormulaVersion formulaVersion) {
        final int frontCount = myconidCount / 2;
        final int backCount = myconidCount - frontCount;
        return guardian(m, formulaVersion).legacyPower() * frontCount
            + slinger(m, formulaVersion).legacyPower() * backCount;
    }

    private int multiply(int value, double multiplier) {
        return PveCharacteristicScaling.multiply(value, multiplier);
    }

    private record Formation(int count) {
        int frontCount() {
            return count / 2;
        }

        int backCount() {
            return count - frontCount();
        }
    }
}
