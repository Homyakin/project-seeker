package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.models.AttackType;

class BattleEventJsonTest {
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID THIRD = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void newCausalEventsRoundTripThroughBattleEventType() throws Exception {
        final List<BattleEvent> events = List.of(
            new BattleEvent.TargetSelected(FIRST, SECOND, THIRD, 2, 7),
            new BattleEvent.AttackIntercepted(FIRST, SECOND, THIRD, 7),
            new BattleEvent.ScalingSkillDamage(
                SECOND,
                FIRST,
                ActiveEnum.DOUBLE_ATTACK,
                Map.of(AttackType.SLASH, 120, AttackType.MAGICAL, 60),
                3,
                5,
                false,
                91,
                409,
                7
            ),
            new BattleEvent.SkillChargeChanged(FIRST, ActiveEnum.BLEEDING, 0, true, 7),
            new BattleEvent.InitiativeDelayed(SECOND, FIRST, ActiveEnum.FEINT, 90, 410, 7),
            new BattleEvent.ThreatChanged(
                FIRST,
                SECOND,
                null,
                -8,
                42,
                BattleEvent.ThreatReason.DAMAGE_TAKEN,
                7
            ),
            new BattleEvent.SkillWindowUsed(FIRST, ActiveEnum.HIT_AND_RUN, 7)
        );

        for (final var event : events) {
            final var json = MAPPER.writerFor(BattleEvent.class).writeValueAsString(event);
            final var restored = MAPPER.readValue(json, BattleEvent.class);

            Assertions.assertEquals(event, restored);
            Assertions.assertTrue(json.contains("\"type\":\"" + event.getClass().getSimpleName() + "\""));
        }
    }

    @Test
    void scalingDamageKeepsMixedBasisImmutableAndOrderedByAttackType() {
        final var source = new EnumMap<AttackType, Integer>(AttackType.class);
        source.put(AttackType.MAGICAL, 40);
        source.put(AttackType.SLASH, 80);
        final var event = new BattleEvent.ScalingSkillDamage(
            SECOND,
            FIRST,
            ActiveEnum.DOUBLE_ATTACK,
            source,
            1,
            2,
            true,
            50,
            450,
            3
        );

        source.put(AttackType.BLUNT, 20);

        Assertions.assertEquals(List.of(AttackType.SLASH, AttackType.MAGICAL), event.basis().keySet().stream().toList());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> event.basis().put(AttackType.PIERCE, 10));
    }
}
