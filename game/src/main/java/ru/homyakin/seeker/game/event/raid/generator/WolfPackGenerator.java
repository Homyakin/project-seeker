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

public class WolfPackGenerator implements RaidBattlePersonageGenerator {
    private static final int BASE_ATTACK = 100;
    private static final int BASE_DEFENSE = 50;
    private static final int BASE_HEALTH = 400;
    private static final int BASE_CRIT_CHANCE = 5;
    private static final int BASE_DODGE_CHANCE = 5;
    private static final double BASE_CRIT_MULTI = 0.5;
    private static final int BASE_SPEED = 300;
    private static final int BASE_THREAT = 30;
    private static final int BASE_ALPHA_ATTACK = 300;
    private static final int BASE_ALPHA_DEFENSE = 100;
    private static final int BASE_ALPHA_HEALTH = 1500;
    private static final int BASE_ALPHA_CRIT_CHANCE = 15;
    private static final int BASE_ALPHA_DODGE_CHANCE = 15;
    private static final double BASE_ALPHA_CRIT_MULTI = 1;
    private static final int BASE_ALPHA_SPEED = 200;
    private static final int BASE_ALPHA_THREAT = 10;
    private static final double EQUIPMENT_POWER_CALIBRATION_CORRECTION = 0.75;

    @Override
    public GeneratedPveEnemies generateCalibrated(
        List<BattlePersonage> personages,
        double powerBonus,
        SkillFormulaVersion formulaVersion
    ) {
        RaidBattlePersonageGenerator.validateInputs(personages, powerBonus, formulaVersion);
        if (formulaVersion == SkillFormulaVersion.SCALING_SKILLS_V3) {
            return generateEquipmentCalibrated(personages, powerBonus, formulaVersion);
        }
        int wolfsCount = personages.size();
        final double groupSizeScaling;
        if (personages.size() <= 13) {
            groupSizeScaling = 1.0 + MathUtils.log(2.2, wolfsCount) * 0.045;
        } else {
            groupSizeScaling = 1.1 + MathUtils.log(10, wolfsCount - 13) * 0.045;
        }
        final var targetPower = personages.stream()
            .mapToDouble(BattlePersonage::legacyPower)
            .sum() * powerBonus * groupSizeScaling;
        final var calibration = PveCharacteristicScaling.search(
            targetPower,
            PveCharacteristicScaling.maxMultiplier(
                BASE_ATTACK, BASE_DEFENSE, BASE_HEALTH,
                BASE_ALPHA_ATTACK, BASE_ALPHA_DEFENSE, BASE_ALPHA_HEALTH
            ),
            multiplier -> totalRaidPower(wolfsCount, multiplier, formulaVersion)
        );
        final var characteristicsMultiplier = calibration.multiplier();

        final var wolfPack = new ArrayList<BattlePersonage>();
        wolfPack.add(alpha(characteristicsMultiplier, formulaVersion));
        for (int i = 0; i < wolfsCount; i++) {
            wolfPack.add(wolf(characteristicsMultiplier, formulaVersion));
        }
        return new GeneratedPveEnemies(wolfPack, calibration);
    }

    private GeneratedPveEnemies generateEquipmentCalibrated(
        List<BattlePersonage> personages,
        double powerBonus,
        SkillFormulaVersion formulaVersion
    ) {
        final int wolfCount = personages.size();
        final double groupSizeScaling;
        if (wolfCount <= 13) {
            groupSizeScaling = 1.0 + MathUtils.log(2.2, wolfCount) * 0.045;
        } else {
            final var boundaryScaling = 1.0 + MathUtils.log(2.2, 13) * 0.045;
            groupSizeScaling = boundaryScaling + MathUtils.log(10, wolfCount / 13.0) * 0.045;
        }
        final var participantAssessment = PveEquipmentPowerSum.sum(personages);
        final var target = participantAssessment * powerBonus * groupSizeScaling
            * EQUIPMENT_POWER_CALIBRATION_CORRECTION;
        final var calibration = PveCharacteristicScaling.search(
            target,
            PveCharacteristicScaling.maxMultiplier(
                BASE_ATTACK, BASE_DEFENSE, BASE_HEALTH,
                BASE_ALPHA_ATTACK, BASE_ALPHA_DEFENSE, BASE_ALPHA_HEALTH
            ),
            multiplier -> RandomUtils.withSeed(0, () -> PveEquipmentPowerSum.sum(
                equipmentPack(wolfCount, multiplier, formulaVersion)))
        );
        final var diagnostics = new PveScalingDiagnostics(PveEquipmentPowerSum.VERSION, participantAssessment, Map.of(
            "enemyCount", (double) wolfCount + 1,
            "wolfCount", (double) wolfCount,
            "alphaCount", 1.0,
            "groupSizeScaling", groupSizeScaling,
            "powerBonus", powerBonus,
            "encounterCorrection", EQUIPMENT_POWER_CALIBRATION_CORRECTION
        ));
        return new GeneratedPveEnemies(
            equipmentPack(wolfCount, calibration.multiplier(), formulaVersion), calibration, Optional.of(diagnostics)
        );
    }

    private List<BattlePersonage> equipmentPack(int wolfCount, double multiplier, SkillFormulaVersion formulaVersion) {
        final var pack = new ArrayList<BattlePersonage>();
        pack.add(alpha(multiplier, formulaVersion));
        for (int index = 0; index < wolfCount; ++index) {
            pack.add(wolf(multiplier, formulaVersion));
        }
        return pack;
    }

    private double totalRaidPower(int wolfsCount, double characteristicsMultiplier, SkillFormulaVersion formulaVersion) {
        return alpha(characteristicsMultiplier, formulaVersion).legacyPower()
            + wolf(characteristicsMultiplier, formulaVersion).legacyPower() * wolfsCount;
    }

    private BattlePersonage alpha(double characteristicsMultiplier, SkillFormulaVersion formulaVersion) {
        return BattlePersonage.forCombat(
            List.of(
                new Item(
                    new ItemObject(
                        null,
                        Set.of(PersonageSlot.MAIN_HAND),
                        Optional.of(
                            new ItemAttack(
                                AttackType.SLASH,
                                1,
                                multiply(BASE_ALPHA_ATTACK, characteristicsMultiplier)
                            )
                        ),
                        Optional.of(
                            new ItemDefense(
                                DefenseType.LEATHER,
                                multiply(BASE_ALPHA_DEFENSE, characteristicsMultiplier)
                            )
                        ),
                        multiply(BASE_ALPHA_HEALTH, characteristicsMultiplier),
                        BASE_ALPHA_CRIT_CHANCE,
                        BASE_ALPHA_DODGE_CHANCE,
                        BASE_ALPHA_CRIT_MULTI,
                        BASE_ALPHA_SPEED,
                        BASE_ALPHA_THREAT,
                        null
                    ),
                    Optional.of(new Modifier(ActiveEnum.BERSERK)),
                    ItemRarity.LEGENDARY
                )
            ),
            Position.FRONT,
            PersonageEffects.EMPTY,
            Optional.empty(),
            formulaVersion
        );
    }

    private BattlePersonage wolf(double characteristicsMultiplier, SkillFormulaVersion formulaVersion) {
        return BattlePersonage.forCombat(
            List.of(
                new Item(
                    new ItemObject(
                        null,
                        Set.of(PersonageSlot.MAIN_HAND),
                        Optional.of(
                            new ItemAttack(
                                AttackType.SLASH,
                                1,
                                multiply(BASE_ATTACK, characteristicsMultiplier)
                            )
                        ),
                        Optional.of(
                            new ItemDefense(
                                DefenseType.LEATHER,
                                multiply(BASE_DEFENSE, characteristicsMultiplier)
                            )
                        ),
                        multiply(BASE_HEALTH, characteristicsMultiplier),
                        BASE_CRIT_CHANCE,
                        BASE_DODGE_CHANCE,
                        BASE_CRIT_MULTI,
                        BASE_SPEED,
                        BASE_THREAT,
                        null
                    ),
                    Optional.of(new Modifier(
                        ActiveEnum.BLEEDING
                    )),
                    ItemRarity.LEGENDARY
                )
            ),
            Position.FRONT,
            PersonageEffects.EMPTY,
            Optional.empty(),
            formulaVersion
        );
    }

    private int multiply(int value, double multiplier) {
        return PveCharacteristicScaling.multiply(value, multiplier);
    }
}
