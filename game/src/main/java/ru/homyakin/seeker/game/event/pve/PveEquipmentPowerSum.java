package ru.homyakin.seeker.game.event.pve;

import java.util.Arrays;
import java.util.List;
import ru.homyakin.seeker.game.battle.BattlePersonage;

/** The agreed sum of personal permanent-equipment assessments, independent of input order. */
public final class PveEquipmentPowerSum {
    public static final String VERSION = "EQUIPMENT_POWER_SUM_V1";

    private PveEquipmentPowerSum() {
    }

    public static double sum(List<BattlePersonage> personages) {
        if (personages == null) {
            throw new IllegalArgumentException("Equipment power requires participants");
        }
        final var powers = new double[personages.size()];
        for (var index = 0; index < personages.size(); ++index) {
            final var personage = personages.get(index);
            if (personage == null) {
                throw new IllegalArgumentException("Equipment power requires non-null participants");
            }
            final var power = personage.equipmentPowerUnscaled();
            if (!Double.isFinite(power) || power < 0) {
                throw new IllegalArgumentException("Equipment power must be finite and nonnegative: " + power);
            }
            powers[index] = power;
        }
        Arrays.sort(powers);
        var sum = 0.0;
        for (final var power : powers) {
            sum += power;
            if (!Double.isFinite(sum)) {
                throw new IllegalArgumentException("Equipment power sum must be finite");
            }
        }
        return sum;
    }
}
