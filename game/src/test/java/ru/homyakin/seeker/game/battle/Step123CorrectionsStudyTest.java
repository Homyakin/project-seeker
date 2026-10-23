package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.utils.RandomUtils;

class Step123CorrectionsStudyTest {
    @Test
    void correctionGridIsExplicitBoundedAndKeepsHistoricalCatalogComparisons() {
        final var cases = Step123CorrectionsStudy.cases();
        Assertions.assertEquals(94, cases.size());
        Assertions.assertEquals(cases.size(), cases.stream().map(Step123CorrectionsStudy.StudyCase::code).distinct().count());
        Assertions.assertEquals(30, cases.stream().filter(value -> value.family().equals("WEAPON")).count());
        Assertions.assertEquals(6, cases.stream().filter(value -> value.family().equals("MOBILE")).count());
        Assertions.assertEquals(20, cases.stream().filter(value -> value.family().equals("REFERENCE")).count());
        Assertions.assertEquals(10, cases.stream().filter(value -> value.family().equals("REFERENCE_MIRROR")).count());
        Assertions.assertEquals(18, cases.stream().filter(value -> value.family().equals("PRESSURE_REAR")).count());
        Assertions.assertEquals(10, cases.stream().filter(value -> value.family().equals("FROZEN_CONTROL")).count());
        for (final var value : cases) {
            Assertions.assertEquals(value.left().size(), value.right().size());
            for (final var team : List.of(value.left(), value.right())) {
                for (final var gear : team) {
                    Assertions.assertEquals(7, gear.items(value.level()).stream()
                        .mapToInt(item -> item.object().slots().size()).sum());
                    if (value.family().equals("WEAPON")) {
                        Assertions.assertTrue(gear.build().assignments().isEmpty());
                    } else {
                        Assertions.assertEquals(EquipmentCatalogVersion.SCALING_V5, gear.catalog());
                    }
                }
            }
        }
        for (final var catalog : List.of(EquipmentCatalogVersion.SCALING_V4, EquipmentCatalogVersion.SCALING_V5)) {
            final var sample = cases.stream().filter(value -> value.family().equals("WEAPON")
                && value.left().getFirst().catalog() == catalog && value.left().size() == 1).findFirst().orElseThrow();
            final var halberd = sample.left().getFirst().items(0).stream()
                .filter(item -> item.object().code().equals("halberd")).findFirst().orElseThrow();
            Assertions.assertEquals(catalog == EquipmentCatalogVersion.SCALING_V4 ? 360 : 420,
                halberd.itemAttacks().getFirst().attack());
            Assertions.assertEquals(Position.MID, sample.left().getFirst().build().position());
            Assertions.assertEquals(Position.MID, sample.right().getFirst().build().position());
        }
        Assertions.assertEquals(Set.of(0, 3, 6), cases.stream().filter(value -> value.family().equals("WEAPON"))
            .map(Step123CorrectionsStudy.StudyCase::level).collect(Collectors.toSet()));
        Assertions.assertEquals(Set.of("BEGINNER", "MID", "STRONG", "EXTENDED", "STRESS"), cases.stream()
            .filter(value -> value.family().startsWith("REFERENCE"))
            .map(Step123CorrectionsStudy.StudyCase::tier).collect(Collectors.toSet()));
    }

    @Test
    void mobileOnAndOffKeepIdenticalGearAndPairedSeedsAgainstCommonFrontPressure() {
        final var mobile = Step123CorrectionsStudy.cases().stream()
            .filter(value -> value.family().equals("MOBILE")).toList();
        Assertions.assertEquals(Set.of(0, 6, 10), mobile.stream()
            .map(Step123CorrectionsStudy.StudyCase::level).collect(Collectors.toSet()));
        for (final var on : mobile.stream().filter(value -> value.disabledLeft().isEmpty()).toList()) {
            final var off = mobile.stream().filter(value -> value.level() == on.level() && !value.disabledLeft().isEmpty())
                .findFirst().orElseThrow();
            Assertions.assertEquals(on.left(), off.left());
            Assertions.assertEquals(on.right(), off.right());
            Assertions.assertEquals(on.seedGroup(), off.seedGroup());
            Assertions.assertEquals(Set.of(ActiveEnum.HIT_AND_RUN), off.disabledLeft());
            Assertions.assertEquals(Set.of("boots", "leather_helm"), on.left().getFirst().build().assignments().keySet());
            Assertions.assertTrue(on.right().stream().allMatch(gear ->
                gear.build().position() == Position.FRONT && gear.build().assignments().isEmpty()));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void movementAuditMapsBothOrientationsAndUpdatesPositionsBetweenEvents(boolean reverse) {
        final var left = UUID.fromString("00000000-0000-0000-0000-000000000001");
        final var right = UUID.fromString("00000000-0000-0000-0000-000000000002");
        final var events = ticks(left, right, reverse);
        events.add(new BattleEvent.PersonageForcedMove(left, right, ActiveEnum.RETREAT, 1, 2));
        events.add(new BattleEvent.PersonageForcedMove(right, left, ActiveEnum.KNOCKBACK, 4, 2));
        events.add(new BattleEvent.MovedTowardEnemy(left, 2, 3));
        events.add(new BattleEvent.PersonageForcedMove(left, left, ActiveEnum.HIT_AND_RUN, 1, 4));

        final var audit = Step123CorrectionsStudy.audit(positions(), events, reverse);

        Assertions.assertEquals(new Step123CorrectionsStudy.MovementAudit(3, 0, 1, 1, 1, 1), audit);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void movementAuditDetectsTheOriginalTwoLineJumpsOnBothSides(boolean reverse) {
        final var left = UUID.fromString("00000000-0000-0000-0000-000000000001");
        final var right = UUID.fromString("00000000-0000-0000-0000-000000000002");
        final var events = ticks(left, right, reverse);
        events.add(new BattleEvent.MovedTowardEnemy(left, 3, 43));
        events.add(new BattleEvent.MovedTowardEnemy(right, 2, 43));
        events.add(new BattleEvent.MovedTowardEnemy(right, 1, 51));
        events.add(new BattleEvent.MovedTowardEnemy(left, 4, 54));
        events.add(new BattleEvent.PersonageForcedMove(right, left, ActiveEnum.RETREAT, 3, 55));
        events.add(new BattleEvent.PersonageForcedMove(left, right, ActiveEnum.RETREAT, 2, 56));

        final var audit = Step123CorrectionsStudy.audit(positions(), events, reverse);

        Assertions.assertEquals(new Step123CorrectionsStudy.MovementAudit(2, 2, 2, 0, 2, 0), audit);
    }

    @Test
    void movementAuditRejectsMissingInitialActorsInsteadOfReportingFalseZeroViolations() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Step123CorrectionsStudy.audit(positions(), List.of(), false));
    }

    @ParameterizedTest
    @CsvSource({"3289266323258170439, false", "6446094721776612200, true"})
    void studyObservationsMatchActualBattleEventsAndMovementAudit(long seed, boolean reverse) {
        final var value = Step123CorrectionsStudy.cases().stream()
            .filter(candidate -> candidate.code().equals("PRESSURE_REAR_N3_LEGENDARY_L10")).findFirst().orElseThrow();
        final var observed = Step123BattleRunner.observe(value.left(), value.right(), value.level(), seed,
            reverse, 10_000, Set.of(), Set.of());
        final var actual = RandomUtils.withSeed(seed, () -> {
            final var left = battleTeam(value.left(), value.level());
            final var right = battleTeam(value.right(), value.level());
            return new Battle(seed).process(reverse ? right : left, reverse ? left : right, 10_000);
        });
        Assertions.assertEquals(actual.rounds(), observed.rounds());
        Assertions.assertEquals(actual.firstWin() != reverse ? "WIN" : "LOSS", observed.outcome());
        Assertions.assertEquals(actual.actionLog().events(), observed.events());
        final var actualPositions = actual.actionLog().events().stream()
            .filter(event -> event instanceof BattleEvent.InitiativeAfterTick tick && tick.round() == 1)
            .map(BattleEvent.InitiativeAfterTick.class::cast)
            .map(tick -> actual.initState().personagesById().get(tick.personageId()))
            .toList();
        final var definitions = new ArrayList<String>();
        for (final boolean firstTeam : List.of(!reverse, reverse)) {
            for (final var initial : actualPositions.stream().filter(initial -> initial.firstTeam() == firstTeam).toList()) {
                definitions.add((firstTeam != reverse ? "LEFT" : "RIGHT") + ":0:ACTUAL:FRONT:" + initial.lineIndex());
            }
        }
        final var observedAudit = Step123CorrectionsStudy.audit(observed.initialPositions(), observed.events(), reverse);
        final var actualAudit = Step123CorrectionsStudy.audit(definitions, actual.actionLog().events(), reverse);
        Assertions.assertEquals(actualAudit, observedAudit);
        Assertions.assertEquals(0, observedAudit.violations());
        Assertions.assertTrue(observedAudit.forcedMoves() > 0);
    }

    private static List<BattlePersonage> battleTeam(List<Step123BattleRunner.Gear> gear, int level) {
        return gear.stream().map(member -> {
            final var build = member.build();
            final var personage = BattlePersonage.forScalingSkillsV2Combat(
                member.items(level), build.position(), Map.of(), Optional.of(build.code())
            );
            personage.setTargetingTactic(build.tactic());
            return personage;
        }).toList();
    }

    private static List<String> positions() {
        return List.of("LEFT:0:GUARD:FRONT:2", "RIGHT:0:BRUISER:FRONT:3");
    }

    private static ArrayList<BattleEvent> ticks(UUID left, UUID right, boolean reverse) {
        return new ArrayList<>(List.of(
            new BattleEvent.InitiativeAfterTick(reverse ? right : left, 100, true, 1),
            new BattleEvent.InitiativeAfterTick(reverse ? left : right, 100, true, 1)
        ));
    }
}
