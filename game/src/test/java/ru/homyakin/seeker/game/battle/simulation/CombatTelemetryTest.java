package ru.homyakin.seeker.game.battle.simulation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.BattleAdvanceDirection;
import ru.homyakin.seeker.game.battle.BattleEvent;
import ru.homyakin.seeker.game.battle.BattleInitState;
import ru.homyakin.seeker.game.battle.BattlePersonageInitSnapshot;
import ru.homyakin.seeker.game.battle.DamageRoll;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.item.models.AttackType;

class CombatTelemetryTest {
    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID UNKNOWN = UUID.fromString("00000000-0000-0000-0000-000000000099");

    @Test
    void eventsAreAttributedToTheSideFromInitialSnapshot() {
        final var events = List.<BattleEvent>of(
            new BattleEvent.DamageReceived(SECOND, FIRST, new DamageRoll(Map.of(AttackType.SLASH, 20), false), 11, 89, 1),
            new BattleEvent.SkillDamage(SECOND, FIRST, ActiveEnum.DOUBLE_ATTACK, AttackType.SLASH, 7, 82, 1),
            new BattleEvent.ScalingSkillDamage(
                SECOND,
                FIRST,
                ActiveEnum.PRECISE_STRIKE,
                Map.of(AttackType.SLASH, 20, AttackType.MAGICAL, 10),
                1,
                2,
                false,
                13,
                69,
                1
            ),
            new BattleEvent.EffectDamage(SECOND, FIRST, ActiveEnum.BLEEDING, AttackType.SLASH, 3, 66, 2),
            new BattleEvent.ScalingSkillDamage(
                SECOND,
                FIRST,
                ActiveEnum.BLEEDING,
                Map.of(AttackType.SLASH, 20),
                1,
                4,
                true,
                5,
                61,
                2
            ),
            new BattleEvent.PersonageHealed(FIRST, ActiveEnum.SELF_HEAL, 4, 90, 2),
            new BattleEvent.AttackIntercepted(SECOND, UNKNOWN, FIRST, 2),
            new BattleEvent.MovedTowardEnemy(FIRST, 1, 2),
            new BattleEvent.PersonageForcedMove(SECOND, FIRST, ActiveEnum.KNOCKBACK, 3, 2),
            new BattleEvent.PersonageForcedMove(SECOND, FIRST, ActiveEnum.RETREAT, 3, 2),
            new BattleEvent.SkillChargeChanged(FIRST, ActiveEnum.BLEEDING, 2, false, 2),
            new BattleEvent.SkillChargeChanged(FIRST, ActiveEnum.BLEEDING, 0, true, 3),
            new BattleEvent.SkillWindowUsed(FIRST, ActiveEnum.PENETRATION, 3),
            new BattleEvent.InitiativeDelayed(SECOND, FIRST, ActiveEnum.FEINT, 12, 488, 3),
            new BattleEvent.TargetSelected(FIRST, SECOND, SECOND, 1, 3),
            new BattleEvent.ThreatChanged(
                FIRST,
                FIRST,
                null,
                5,
                65,
                BattleEvent.ThreatReason.NORMAL_HIT,
                3
            ),
            new BattleEvent.ThreatChanged(
                FIRST,
                SECOND,
                null,
                50,
                60,
                BattleEvent.ThreatReason.KILL,
                3
            ),
            new BattleEvent.ThreatChanged(
                FIRST,
                SECOND,
                ActiveEnum.THORNS,
                -8,
                52,
                BattleEvent.ThreatReason.DAMAGE_TAKEN,
                3
            ),
            new BattleEvent.DamageReceived(FIRST, SECOND, new DamageRoll(Map.of(AttackType.BLUNT, 9), false), 9, 81, 4),
            new BattleEvent.PersonageHealed(SECOND, ActiveEnum.SELF_HEAL, 2, 63, 4),
            new BattleEvent.TargetSelected(SECOND, FIRST, FIRST, 1, 4),
            new BattleEvent.InitiativeDelayed(FIRST, SECOND, ActiveEnum.FEINT, 6, 300, 4),
            new BattleEvent.SkillDamage(FIRST, UNKNOWN, ActiveEnum.FEINT, AttackType.PIERCE, 999, 0, 4)
        );

        final var result = CombatSimulator.summarizeEvents(initState(), events);

        Assertions.assertEquals(
            new CombatSimulator.EventTeamSample(11, 20, 8, 4, 1, 2, 1, 1, 12, 1, 55, 8, 5, 50, -8),
            result.firstTeam()
        );
        Assertions.assertEquals(
            new CombatSimulator.EventTeamSample(9, 0, 0, 2, 0, 1, 0, 0, 6, 1, 0, 0, 0, 0, 0),
            result.secondTeam()
        );
    }

    @Test
    void legacyEventsRecoverTargetSelectionsAndThreatInTheOriginalOrder() {
        final var events = List.<BattleEvent>of(
            new BattleEvent.DamageReceived(
                SECOND,
                FIRST,
                new DamageRoll(Map.of(AttackType.SLASH, 20), false),
                10,
                90,
                1
            ),
            new BattleEvent.SkillDamage(
                FIRST,
                SECOND,
                ActiveEnum.COUNTER_ATTACK,
                AttackType.BLUNT,
                10,
                90,
                1
            ),
            new BattleEvent.PersonageHealed(FIRST, ActiveEnum.SELF_HEAL, 1, 91, 1),
            new BattleEvent.AttackDodged(SECOND, FIRST, 2),
            new BattleEvent.SkillDamage(
                FIRST,
                SECOND,
                ActiveEnum.PRECISE_STRIKE,
                AttackType.BLUNT,
                90,
                0,
                2
            ),
            new BattleEvent.PersonageDefeated(FIRST, SECOND, 2),
            new BattleEvent.DamageReceived(
                FIRST,
                SECOND,
                new DamageRoll(Map.of(AttackType.BLUNT, 20), false),
                10,
                80,
                3
            )
        );

        final var result = CombatSimulator.summarizeEvents(initState(), events);

        Assertions.assertAll(
            () -> Assertions.assertEquals(1, result.firstTeam().targetSelections()),
            () -> Assertions.assertEquals(5, result.firstTeam().threatGained()),
            () -> Assertions.assertEquals(5, result.firstTeam().normalHitThreatDelta()),
            () -> Assertions.assertEquals(5, result.firstTeam().threatLost()),
            () -> Assertions.assertEquals(-5, result.firstTeam().damageTakenThreatDelta()),
            () -> Assertions.assertEquals(2, result.secondTeam().targetSelections()),
            () -> Assertions.assertEquals(55, result.secondTeam().threatGained()),
            () -> Assertions.assertEquals(5, result.secondTeam().normalHitThreatDelta()),
            () -> Assertions.assertEquals(50, result.secondTeam().killThreatDelta()),
            () -> Assertions.assertEquals(0, result.secondTeam().threatLost())
        );
    }

    @Test
    void markdownKeepsOriginalSummaryAndAddsCausalMetrics() {
        final var evaluated = new CombatSimulationReport.TeamMetrics(
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            8,
            9,
            10,
            11,
            12,
            13,
            14,
            15,
            16,
            17,
            18,
            19,
            20,
            21
        );
        final var opponents = new CombatSimulationReport.TeamMetrics(1, 2, 3, 4, 5, 6);
        final var source = new CombatSimulationReport.ParticipantRef(
            CombatSimulationReport.TeamSide.EVALUATED,
            0,
            "Тактик"
        );
        final var target = new CombatSimulationReport.ParticipantRef(
            CombatSimulationReport.TeamSide.OPPONENTS,
            1,
            "Громила"
        );
        final var report = new CombatSimulationReport(
            "RAID",
            "LOADOUT",
            "COMPOSITION",
            10,
            3,
            42,
            100,
            1_000,
            50,
            0.5,
            new CombatSimulationReport.ConfidenceInterval(0.4, 0.6),
            12,
            20,
            evaluated,
            opponents,
            List.of(
                new CombatSimulationReport.CausalMetric(
                    CombatSimulationReport.CausalMetricType.TARGET_SELECTION,
                    source,
                    Optional.of(target),
                    Optional.empty(),
                    Optional.empty(),
                    1.5
                ),
                new CombatSimulationReport.CausalMetric(
                    CombatSimulationReport.CausalMetricType.THREAT_CHANGE,
                    source,
                    Optional.of(target),
                    Optional.of(ActiveEnum.TEMPO_BREAK),
                    Optional.of(BattleEvent.ThreatReason.DAMAGE_TAKEN),
                    -8
                )
            )
        );

        final var markdown = report.markdown();

        Assertions.assertTrue(markdown.contains("| Команда | Выжившие | Остаток здоровья"));
        Assertions.assertTrue(markdown.contains("| Команда | Обычный урон | Урон умений | Периодический урон"));
        Assertions.assertTrue(markdown.contains(
            "| Команда | Изменение угрозы: попадание | Изменение угрозы: устранение"
        ));
        Assertions.assertTrue(markdown.contains(
            "| Проверяемая | 7.00 | 8.00 | 9.00 | 10.00 | 11.00 | 12.00 | 13.00 | 14.00 |"
        ));
        Assertions.assertTrue(markdown.contains(
            "| Проверяемая | 15.00 | 16.00 | 17.00 | 18.00 |"
        ));
        Assertions.assertTrue(markdown.contains(
            "| Проверяемая | 19.00 | 20.00 | 21.00 |"
        ));
        Assertions.assertTrue(markdown.contains(
            "| Противники | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 |"
        ));
        Assertions.assertTrue(markdown.contains(
            "| Выбор цели | Проверяемая №1 · Тактик | — | Противники №2 · Громила | — | 1.50 |"
        ));
        Assertions.assertTrue(markdown.contains(
            "| Изменение угрозы | Проверяемая №1 · Тактик | Срыв темпа | "
                + "Противники №2 · Громила | Полученный урон | -8.00 |"
        ));
        Assertions.assertEquals(0, opponents.averageSkillWindowsUsed());
        Assertions.assertEquals(0, opponents.averageNormalHitThreatDelta());
        Assertions.assertEquals(0, opponents.averageKillThreatDelta());
        Assertions.assertEquals(0, opponents.averageDamageTakenThreatDelta());
    }

    private static BattleInitState initState() {
        return new BattleInitState(
            List.of(),
            Map.of(
                FIRST, snapshot(FIRST, true),
                SECOND, snapshot(SECOND, false)
            )
        );
    }

    private static BattlePersonageInitSnapshot snapshot(UUID id, boolean firstTeam) {
        return new BattlePersonageInitSnapshot(
            id,
            Optional.empty(),
            firstTeam,
            100,
            0,
            firstTeam ? BattleAdvanceDirection.TOWARD_SECOND_TEAM : BattleAdvanceDirection.TOWARD_FIRST_TEAM,
            100,
            0,
            1,
            10,
            80,
            TargetingTactic.THREAT,
            List.of(),
            List.of(),
            0,
            0,
            1.2,
            List.of(),
            Map.of(),
            Map.of()
        );
    }
}
