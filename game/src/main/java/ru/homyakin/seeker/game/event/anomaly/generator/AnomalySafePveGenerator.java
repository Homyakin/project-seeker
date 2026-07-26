package ru.homyakin.seeker.game.event.anomaly.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveFormation;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveTemplate;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.RandomUtils;

/**
 * Builds scaled enemies for anomaly safe-mode PvE.
 *
 * <p>Enemy count by party size:
 * 1→2, 2→2–3, 3→3, 4→4, 5→5.
 */
public class AnomalySafePveGenerator {
    private static final int BASE_ATTACK = 90;
    private static final int BASE_DEFENSE = 100;
    private static final int BASE_HEALTH = 340;
    private static final int BASE_CRIT_CHANCE = 5;
    private static final int BASE_DODGE_CHANCE = 4;
    private static final double BASE_CRIT_MULTI = 0.4;
    private static final int BASE_SPEED = 180;
    private static final int BASE_THREAT_FRONT = 30;
    private static final int BASE_THREAT_MID = 20;
    private static final int BASE_THREAT_BACK = 12;
    private static final double SAFE_POWER_RATIO = 0.92;

    public List<BattlePersonage> generate(AnomalyPveTemplate template, List<BattlePersonage> players) {
        final int enemyCount = enemyCount(players.size());
        final double targetPower = players.stream().mapToDouble(BattlePersonage::power).sum() * SAFE_POWER_RATIO;
        final double multiplier = characteristicsMultiplier(template, enemyCount, targetPower);
        final var positions = positionsFor(template.formation(), enemyCount);
        final var enemies = new ArrayList<BattlePersonage>(enemyCount);
        for (final var position : positions) {
            enemies.add(enemy(template, position, multiplier));
        }
        return enemies;
    }

    static int enemyCount(int players) {
        return switch (Math.max(1, players)) {
            case 1 -> 2;
            case 2 -> RandomUtils.getInInterval(2, 3);
            case 3 -> 3;
            case 4 -> 4;
            default -> 5;
        };
    }

    private List<Position> positionsFor(AnomalyPveFormation formation, int count) {
        return switch (formation) {
            case STRONG_FRONT_LINE -> List.copyOf(java.util.Collections.nCopies(count, Position.FRONT));
            case MID_LINE -> {
                final var list = new ArrayList<Position>(count);
                list.add(Position.FRONT);
                for (int i = 1; i < count; i++) {
                    list.add(Position.MID);
                }
                yield list;
            }
            case STRONG_BACK_LINE -> {
                final var list = new ArrayList<Position>(count);
                list.add(Position.FRONT);
                for (int i = 1; i < count; i++) {
                    list.add(Position.BACK);
                }
                yield list;
            }
            case SPLIT_WINGS -> {
                final int front = (count + 1) / 2;
                final var list = new ArrayList<Position>(count);
                for (int i = 0; i < front; i++) {
                    list.add(Position.FRONT);
                }
                for (int i = front; i < count; i++) {
                    list.add(Position.BACK);
                }
                yield list;
            }
        };
    }

    private BattlePersonage enemy(AnomalyPveTemplate template, Position position, double multiplier) {
        final int range = template.attackType() == AttackType.MAGICAL || position == Position.BACK ? 2 : 1;
        final int threat = switch (position) {
            case FRONT -> BASE_THREAT_FRONT;
            case MID -> BASE_THREAT_MID;
            case BACK -> BASE_THREAT_BACK;
        };
        return new BattlePersonage(
            List.of(
                new Item(
                    new ItemObject(
                        null,
                        Set.of(PersonageSlot.MAIN_HAND),
                        Optional.of(new ItemAttack(
                            template.attackType(),
                            range,
                            multiply(BASE_ATTACK, multiplier)
                        )),
                        Optional.of(new ItemDefense(
                            template.defenseType(),
                            multiply(BASE_DEFENSE, multiplier)
                        )),
                        multiply(BASE_HEALTH, multiplier),
                        BASE_CRIT_CHANCE,
                        BASE_DODGE_CHANCE,
                        BASE_CRIT_MULTI,
                        BASE_SPEED,
                        threat,
                        null
                    ),
                    Optional.of(new Modifier(skillFor(template.formation(), position))),
                    ItemRarity.LEGENDARY
                )
            ),
            position
        );
    }

    private ActiveEnum skillFor(AnomalyPveFormation formation, Position position) {
        return switch (formation) {
            case STRONG_FRONT_LINE -> ActiveEnum.BERSERK;
            case MID_LINE -> ActiveEnum.HIT_AND_RUN;
            case STRONG_BACK_LINE -> position == Position.FRONT ? ActiveEnum.KNOCKBACK : ActiveEnum.PRECISE_STRIKE;
            case SPLIT_WINGS -> position == Position.FRONT ? ActiveEnum.THORNS : ActiveEnum.BLEEDING;
        };
    }

    private double characteristicsMultiplier(AnomalyPveTemplate template, int enemyCount, double targetPower) {
        double low = 0;
        double high = 1;
        if (totalPower(template, enemyCount, low) >= targetPower) {
            return low;
        }
        while (totalPower(template, enemyCount, high) < targetPower) {
            low = high;
            high *= 2;
        }
        for (int i = 0; i < 32; i++) {
            final var mid = (low + high) / 2;
            if (totalPower(template, enemyCount, mid) >= targetPower) {
                high = mid;
            } else {
                low = mid;
            }
        }
        final var lowDiff = Math.abs(totalPower(template, enemyCount, low) - targetPower);
        final var highDiff = Math.abs(totalPower(template, enemyCount, high) - targetPower);
        return lowDiff < highDiff ? low : high;
    }

    private double totalPower(AnomalyPveTemplate template, int enemyCount, double multiplier) {
        return positionsFor(template.formation(), enemyCount).stream()
            .mapToDouble(position -> enemy(template, position, multiplier).power())
            .sum();
    }

    private int multiply(int value, double multiplier) {
        return Math.max(1, (int) Math.ceil(value * multiplier));
    }
}
