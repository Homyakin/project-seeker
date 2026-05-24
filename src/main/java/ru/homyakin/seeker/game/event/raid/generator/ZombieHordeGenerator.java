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
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Zombie Horde raid: slow, heavily PLATE-armoured undead.
 * Each zombie carries a single randomly chosen BLUNT or SLASH attack —
 * the 50/50 mix in practice matches the {@code power()} formula's averaged
 * damage-reduction assumption, keeping the difficulty calibration accurate.
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

    @Override
    public List<BattlePersonage> generate(List<BattlePersonage> personages, double powerBonus) {
        final int zombieCount = personages.size();
        final double groupSizeScaling;
        if (personages.size() <= 10) {
            groupSizeScaling = 1.0 + MathUtils.log(2.2, zombieCount) * GROUP_SCALE_COEF;
        } else if (personages.size() <= 13) {
            groupSizeScaling = 1.0 + MathUtils.log(4, zombieCount) * GROUP_SCALE_COEF;
        } else {
            groupSizeScaling = 1.0 + MathUtils.log(8, zombieCount) * GROUP_SCALE_COEF;
        }
        final var targetPower = personages.stream()
            .mapToDouble(BattlePersonage::power)
            .sum() * powerBonus * groupSizeScaling;
        final var multiplier = characteristicsMultiplier(zombieCount, targetPower);

        final var horde = new ArrayList<BattlePersonage>();
        horde.add(boss(multiplier, randomAttack()));
        for (int i = 0; i < zombieCount; i++) {
            horde.add(zombie(multiplier, randomAttack()));
        }
        return horde;
    }

    // -------------------------------------------------------------------------
    // Unit factories
    // -------------------------------------------------------------------------

    private BattlePersonage zombie(double m, AttackType attackType) {
        return new BattlePersonage(
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
            Position.FRONT
        );
    }

    private BattlePersonage boss(double m, AttackType attackType) {
        return new BattlePersonage(
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
            Position.FRONT
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

    private double characteristicsMultiplier(int zombieCount, double targetPower) {
        double low = 0;
        double high = 1;

        if (totalRaidPower(zombieCount, low) >= targetPower) {
            return low;
        }

        while (totalRaidPower(zombieCount, high) < targetPower) {
            low = high;
            high *= 2;
        }

        for (int i = 0; i < 32; i++) {
            final var mid = (low + high) / 2;
            if (totalRaidPower(zombieCount, mid) >= targetPower) {
                high = mid;
            } else {
                low = mid;
            }
        }
        return closestMultiplier(zombieCount, targetPower, low, high);
    }

    private double closestMultiplier(int zombieCount, double targetPower, double low, double high) {
        final var lowDiff = Math.abs(totalRaidPower(zombieCount, low) - targetPower);
        final var highDiff = Math.abs(totalRaidPower(zombieCount, high) - targetPower);
        return lowDiff < highDiff ? low : high;
    }

    private double totalRaidPower(int zombieCount, double m) {
        return boss(m, AttackType.SLASH).power() + zombie(m, AttackType.SLASH).power() * zombieCount;
    }

    private int multiply(int value, double multiplier) {
        return Math.max(1, (int) Math.ceil(value * multiplier));
    }
}
