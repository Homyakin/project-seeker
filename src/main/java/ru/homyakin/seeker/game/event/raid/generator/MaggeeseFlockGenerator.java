package ru.homyakin.seeker.game.event.raid.generator;

import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.MathUtils;

import java.util.ArrayList;
import java.util.List;
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
 *       {@link ActiveEnum#FEINT}, dodging retaliations unpredictably.</li>
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

    /**
     * Correction applied to the calibration target before the binary search.
     * CLOTH defence is unusually strong against BLUNT and MAGICAL (the two most common
     * catalogue attack types), so {@code power()} underestimates effective durability.
     * Tune this so that group-3+ winrates at level 10 sit near 50-60 %.
     */
    private static final double POWER_CALIBRATION_CORRECTION = 0.75;

    @Override
    public List<BattlePersonage> generate(List<BattlePersonage> personages, double powerBonus) {
        final int flockSize = Math.max(2, personages.size());
        final double groupSizeScaling;
        if (flockSize <= 10) {
            groupSizeScaling = 1.0 + MathUtils.log(2.2, flockSize) * GROUP_SCALE_COEF;
        } else {
            groupSizeScaling = 1.0 + MathUtils.log(4, flockSize) * GROUP_SCALE_COEF;
        }
        final var targetPower = personages.stream()
            .mapToDouble(BattlePersonage::power)
            .sum() * powerBonus * groupSizeScaling * POWER_CALIBRATION_CORRECTION;
        final var multiplier = characteristicsMultiplier(flockSize, targetPower);

        final int frontCount = flockSize / 2;
        final int midCount = flockSize - frontCount;

        final var flock = new ArrayList<BattlePersonage>();
        for (int i = 0; i < frontCount; i++) {
            flock.add(charger(multiplier));
        }
        for (int i = 0; i < midCount; i++) {
            flock.add(mageese(multiplier));
        }
        return flock;
    }

    // -------------------------------------------------------------------------
    // Unit factories
    // -------------------------------------------------------------------------

    private BattlePersonage charger(double m) {
        return new BattlePersonage(
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
            Position.FRONT
        );
    }

    private BattlePersonage mageese(double m) {
        return new BattlePersonage(
            List.of(
                new Item(
                    new ItemObject(
                        null,
                        Set.of(PersonageSlot.MAIN_HAND),
                        Optional.of(new ItemAttack(AttackType.MAGICAL, 1, multiply(BASE_MAGEESE_ATTACK, m))),
                        Optional.of(new ItemDefense(DefenseType.CLOTH, multiply(BASE_MAGEESE_DEFENSE, m))),
                        multiply(BASE_MAGEESE_HEALTH, m),
                        BASE_MAGEESE_CRIT_CHANCE, BASE_MAGEESE_DODGE_CHANCE, BASE_MAGEESE_CRIT_MULTI,
                        BASE_MAGEESE_SPEED, BASE_MAGEESE_THREAT, null
                    ),
                    Optional.of(new Modifier(ActiveEnum.HIT_AND_RUN)),
                    ItemRarity.LEGENDARY
                )
            ),
            Position.MID
        );
    }

    // -------------------------------------------------------------------------
    // Power calibration (binary search)
    // -------------------------------------------------------------------------

    private double characteristicsMultiplier(int flockSize, double targetPower) {
        double low = 0;
        double high = 1;

        if (totalRaidPower(flockSize, low) >= targetPower) {
            return low;
        }

        while (totalRaidPower(flockSize, high) < targetPower) {
            low = high;
            high *= 2;
        }

        for (int i = 0; i < 32; i++) {
            final var mid = (low + high) / 2;
            if (totalRaidPower(flockSize, mid) >= targetPower) {
                high = mid;
            } else {
                low = mid;
            }
        }
        return closestMultiplier(flockSize, targetPower, low, high);
    }

    private double closestMultiplier(int flockSize, double targetPower, double low, double high) {
        final var lowDiff = Math.abs(totalRaidPower(flockSize, low) - targetPower);
        final var highDiff = Math.abs(totalRaidPower(flockSize, high) - targetPower);
        return lowDiff < highDiff ? low : high;
    }

    private double totalRaidPower(int flockSize, double m) {
        final int frontCount = flockSize / 2;
        final int midCount = flockSize - frontCount;
        return charger(m).power() * frontCount + mageese(m).power() * midCount;
    }

    private int multiply(int value, double multiplier) {
        return Math.max(1, (int) Math.ceil(value * multiplier));
    }
}
