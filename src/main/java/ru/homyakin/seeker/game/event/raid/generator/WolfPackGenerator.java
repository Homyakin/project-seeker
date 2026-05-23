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

import java.util.ArrayList;
import java.util.List;
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

    @Override
    public List<BattlePersonage> generate(List<BattlePersonage> personages, double powerBonus) {
        int wolfsCount = 3 + personages.size() / 3;
        final var targetPower = personages.stream()
            .mapToDouble(BattlePersonage::power)
            .sum() * powerBonus;
        final var characteristicsMultiplier = characteristicsMultiplier(wolfsCount, targetPower);

        final var wolfPack = new ArrayList<BattlePersonage>();
        wolfPack.add(alpha(characteristicsMultiplier));
        for (int i = 0; i < wolfsCount; i++) {
            wolfPack.add(wolf(characteristicsMultiplier));
        }
        return wolfPack;
    }

    private double characteristicsMultiplier(int wolfsCount, double targetPower) {
        double low = 0;
        double high = 1;

        if (totalRaidPower(wolfsCount, low) >= targetPower) {
            return low;
        }

        while (totalRaidPower(wolfsCount, high) < targetPower) {
            low = high;
            high *= 2;
        }

        for (int i = 0; i < 32; i++) {
            final var mid = (low + high) / 2;
            if (totalRaidPower(wolfsCount, mid) >= targetPower) {
                high = mid;
            } else {
                low = mid;
            }
        }
        return closestMultiplier(wolfsCount, targetPower, low, high);
    }

    private double closestMultiplier(int wolfsCount, double targetPower, double low, double high) {
        final var lowDiff = Math.abs(totalRaidPower(wolfsCount, low) - targetPower);
        final var highDiff = Math.abs(totalRaidPower(wolfsCount, high) - targetPower);
        return lowDiff < highDiff ? low : high;
    }

    private double totalRaidPower(int wolfsCount, double characteristicsMultiplier) {
        return alpha(characteristicsMultiplier).power() + wolf(characteristicsMultiplier).power() * wolfsCount;
    }

    private BattlePersonage alpha(double characteristicsMultiplier) {
        return new BattlePersonage(
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
            Position.FRONT
        );
    }

    private BattlePersonage wolf(double characteristicsMultiplier) {
        return new BattlePersonage(
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
            Position.FRONT
        );
    }

    private int multiply(int value, double multiplier) {
        return Math.max(1, (int) Math.ceil(value * multiplier));
    }
}
