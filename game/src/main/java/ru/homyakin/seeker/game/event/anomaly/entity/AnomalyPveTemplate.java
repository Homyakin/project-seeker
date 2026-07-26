package ru.homyakin.seeker.game.event.anomaly.entity;

import java.util.Arrays;
import java.util.Optional;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.utils.RandomUtils;

/**
 * Four safe-mode PvE encounters covering every {@link AttackType} and {@link DefenseType}.
 */
public enum AnomalyPveTemplate {
    ASH_WIND("ash-wind", AttackType.SLASH, DefenseType.CLOTH, AnomalyPveFormation.MID_LINE),
    SHADOW_NEEDLES("shadow-needles", AttackType.PIERCE, DefenseType.LEATHER, AnomalyPveFormation.SPLIT_WINGS),
    IRON_PHALANX("iron-phalanx", AttackType.BLUNT, DefenseType.PLATE, AnomalyPveFormation.STRONG_FRONT_LINE),
    CRYSTAL_STORM("crystal-storm", AttackType.MAGICAL, DefenseType.ARCANE, AnomalyPveFormation.STRONG_BACK_LINE),
    ;

    private final String code;
    private final AttackType attackType;
    private final DefenseType defenseType;
    private final AnomalyPveFormation formation;

    AnomalyPveTemplate(
        String code,
        AttackType attackType,
        DefenseType defenseType,
        AnomalyPveFormation formation
    ) {
        this.code = code;
        this.attackType = attackType;
        this.defenseType = defenseType;
        this.formation = formation;
    }

    public String code() {
        return code;
    }

    public AttackType attackType() {
        return attackType;
    }

    public DefenseType defenseType() {
        return defenseType;
    }

    public AnomalyPveFormation formation() {
        return formation;
    }

    public static AnomalyPveTemplate random() {
        final var values = values();
        return values[RandomUtils.getInInterval(0, values.length - 1)];
    }

    public static Optional<AnomalyPveTemplate> findByCode(String code) {
        return Arrays.stream(values())
            .filter(it -> it.code.equals(code))
            .findFirst();
    }
}
