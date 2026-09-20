package ru.homyakin.seeker.game.battle.simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import ru.homyakin.seeker.game.battle.Battle;
import ru.homyakin.seeker.game.battle.BattleEvent;
import ru.homyakin.seeker.game.battle.BattleInitState;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.simulation.CombatSimulationReport.CausalMetric;
import ru.homyakin.seeker.game.battle.simulation.CombatSimulationReport.CausalMetricType;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.simulation.CombatSimulationReport.ConfidenceInterval;
import ru.homyakin.seeker.game.battle.simulation.CombatSimulationReport.ParticipantRef;
import ru.homyakin.seeker.game.battle.simulation.CombatSimulationReport.StartingPosition;
import ru.homyakin.seeker.game.battle.simulation.CombatSimulationReport.TeamSide;
import ru.homyakin.seeker.game.battle.simulation.CombatSimulationReport.TeamMetrics;
import ru.homyakin.seeker.utils.RandomUtils;

public final class CombatSimulator {
    private static final double Z_95 = 1.959963984540054;
    private static final long GOLDEN_GAMMA = 0x9e3779b97f4a7c15L;
    private static final Comparator<CausalMetricKey> CAUSAL_METRIC_ORDER = Comparator
        .comparingInt((CausalMetricKey key) -> key.type().ordinal())
        .thenComparingInt(key -> key.source().side().ordinal())
        .thenComparingInt(key -> key.source().index())
        .thenComparingInt(key -> key.skill().map(Enum::ordinal).orElse(-1))
        .thenComparingInt(key -> key.target().map(target -> target.side().ordinal()).orElse(-1))
        .thenComparingInt(key -> key.target().map(ParticipantRef::index).orElse(-1))
        .thenComparingInt(key -> key.threatReason().map(Enum::ordinal).orElse(-1));

    public CombatSimulationReport run(CombatSimulationRequest request) {
        var wins = 0;
        final var rounds = new ArrayList<Integer>(request.iterations());
        final var iterationWins = new ArrayList<Boolean>(request.iterations());
        final var evaluatedTotals = new MutableTeamTotals();
        final var opponentTotals = new MutableTeamTotals();
        final var causalTotals = new MutableCausalTotals();
        List<StartingPosition> startingPositions = null;

        for (int iteration = 0; iteration < request.iterations(); iteration++) {
            final long iterationSeed = iterationSeed(request.seed(), iteration);
            final var sample = RandomUtils.withSeed(
                iterationSeed,
                () -> runBattle(request, iterationSeed)
            );
            if (sample.evaluatedTeamWin()) {
                wins++;
            }
            iterationWins.add(sample.evaluatedTeamWin());
            rounds.add(sample.rounds());
            evaluatedTotals.add(sample.evaluatedTeam());
            opponentTotals.add(sample.opponents());
            causalTotals.add(sample.causalMetrics());
            if (startingPositions == null) {
                startingPositions = sample.startingPositions();
            } else if (!startingPositions.equals(sample.startingPositions())) {
                throw new IllegalStateException(
                    "Starting positions changed within simulation series: " + request.composition()
                );
            }
        }

        final double winRate = (double) wins / request.iterations();
        return new CombatSimulationReport(
            request.raidType(),
            request.loadout(),
            request.composition(),
            request.difficulty(),
            request.partySize(),
            request.seed(),
            request.iterations(),
            request.maxRounds(),
            wins,
            winRate,
            wilson95(wins, request.iterations()),
            median(rounds),
            percentileNearestRank(rounds, 0.95),
            evaluatedTotals.average(request.iterations()),
            opponentTotals.average(request.iterations()),
            Objects.requireNonNull(startingPositions),
            causalTotals.average(request.iterations()),
            iterationWins
        );
    }

    private static BattleSample runBattle(CombatSimulationRequest request, long iterationSeed) {
        final var teams = request.teamsFactory().get();
        if (teams.evaluatedTeam().size() != request.partySize()) {
            throw new IllegalArgumentException(
                "Expected evaluated party size %d, got %d"
                    .formatted(request.partySize(), teams.evaluatedTeam().size())
            );
        }
        final var result = new Battle(iterationSeed).process(
            teams.evaluatedTeam(),
            teams.opponents(),
            request.maxRounds()
        );
        final var participants = participantRefs(teams);
        final var eventSamples = summarizeEvents(
            result.initState(),
            result.actionLog().events(),
            participants
        );
        final var evaluated = snapshot(teams.evaluatedTeam()).withEvents(eventSamples.firstTeam());
        final var opponents = snapshot(teams.opponents()).withEvents(eventSamples.secondTeam());
        return new BattleSample(
            result.firstWin(),
            result.rounds(),
            evaluated.withDamageDealt(opponents.damageTaken()),
            opponents.withDamageDealt(evaluated.damageTaken()),
            startingPositions(result.initState(), teams, participants),
            eventSamples.causalMetrics()
        );
    }

    private static List<StartingPosition> startingPositions(
        BattleInitState initState,
        CombatSimulationTeams teams,
        Map<UUID, ParticipantRef> participants
    ) {
        final var requestedLines = new HashMap<UUID, ru.homyakin.seeker.game.battle.Position>();
        teams.evaluatedTeam().forEach(personage ->
            requestedLines.put(personage.id(), personage.startPosition())
        );
        teams.opponents().forEach(personage ->
            requestedLines.put(personage.id(), personage.startPosition())
        );
        return participants.entrySet().stream()
            .sorted(Comparator
                .comparingInt((Map.Entry<UUID, ParticipantRef> entry) -> entry.getValue().side().ordinal())
                .thenComparingInt(entry -> entry.getValue().index()))
            .map(entry -> {
                final var snapshot = Objects.requireNonNull(initState.personagesById().get(entry.getKey()));
                final var actualLine = initState.lines().stream()
                    .filter(line -> line.lineIndex() == snapshot.lineIndex())
                    .findFirst()
                    .orElseThrow();
                final int distanceToNearestEnemy = initState.personagesById().values().stream()
                    .filter(other -> other.firstTeam() != snapshot.firstTeam())
                    .mapToInt(other -> Math.abs(other.lineIndex() - snapshot.lineIndex()))
                    .min()
                    .orElseThrow();
                return new StartingPosition(
                    entry.getValue(),
                    Objects.requireNonNull(requestedLines.get(entry.getKey())),
                    actualLine.position(),
                    snapshot.lineIndex(),
                    distanceToNearestEnemy,
                    snapshot.range()
                );
            })
            .toList();
    }

    private static Map<UUID, ParticipantRef> participantRefs(CombatSimulationTeams teams) {
        final var result = new LinkedHashMap<UUID, ParticipantRef>();
        addParticipantRefs(result, teams.evaluatedTeam(), TeamSide.EVALUATED);
        addParticipantRefs(result, teams.opponents(), TeamSide.OPPONENTS);
        return Map.copyOf(result);
    }

    private static void addParticipantRefs(
        Map<UUID, ParticipantRef> result,
        List<BattlePersonage> team,
        TeamSide side
    ) {
        for (int index = 0; index < team.size(); index++) {
            final var personage = team.get(index);
            result.put(
                personage.id(),
                new ParticipantRef(side, index, personage.name().orElse("участник " + (index + 1)))
            );
        }
    }

    private static TeamSample snapshot(List<BattlePersonage> team) {
        final long maxHealth = team.stream().mapToLong(BattlePersonage::maxHealth).sum();
        final long remainingHealth = team.stream().mapToLong(BattlePersonage::health).sum();
        final double remainingHealthPercent = maxHealth == 0
            ? 0
            : remainingHealth * 100.0 / maxHealth;
        return new TeamSample(
            (int) team.stream().filter(BattlePersonage::isAlive).count(),
            remainingHealth,
            remainingHealthPercent,
            0,
            team.stream().mapToLong(BattlePersonage::actualDamageTaken).sum(),
            team.stream().mapToLong(it -> it.battlePersonageStats().turnsCount()).sum(),
            EventTeamSample.EMPTY
        );
    }

    static EventSamples summarizeEvents(BattleInitState initState, List<BattleEvent> events) {
        return summarizeEvents(initState, events, participantRefs(initState));
    }

    private static Map<UUID, ParticipantRef> participantRefs(BattleInitState initState) {
        final var result = new HashMap<UUID, ParticipantRef>();
        for (final var side : TeamSide.values()) {
            final boolean firstTeam = side == TeamSide.EVALUATED;
            final var participants = initState.personagesById().values().stream()
                .filter(personage -> personage.firstTeam() == firstTeam)
                .sorted(Comparator.comparingInt(ru.homyakin.seeker.game.battle.BattlePersonageInitSnapshot::lineIndex)
                    .thenComparing(ru.homyakin.seeker.game.battle.BattlePersonageInitSnapshot::id))
                .toList();
            for (int index = 0; index < participants.size(); index++) {
                final var personage = participants.get(index);
                result.put(
                    personage.id(),
                    new ParticipantRef(side, index, personage.name().orElse("участник " + (index + 1)))
                );
            }
        }
        return Map.copyOf(result);
    }

    private static EventSamples summarizeEvents(
        BattleInitState initState,
        List<BattleEvent> events,
        Map<UUID, ParticipantRef> participants
    ) {
        final var firstTeam = new MutableEventTeamSample();
        final var secondTeam = new MutableEventTeamSample();
        final var causalMetrics = new HashMap<CausalMetricKey, Long>();
        final boolean hasExplicitTargetSelections = events.stream()
            .anyMatch(BattleEvent.TargetSelected.class::isInstance);
        final boolean hasExplicitThreatChanges = events.stream()
            .anyMatch(BattleEvent.ThreatChanged.class::isInstance);
        for (final var event : events) {
            if (event instanceof BattleEvent.DamageReceived damage) {
                metricsFor(initState, damage.attackerId(), firstTeam, secondTeam)
                    .ifPresent(metrics -> metrics.normalDamageDealt += damage.damageTaken());
                addCausal(
                    causalMetrics,
                    participants,
                    CausalMetricType.NORMAL_DAMAGE,
                    damage.attackerId(),
                    damage.targetId(),
                    null,
                    null,
                    damage.damageTaken()
                );
            } else if (event instanceof BattleEvent.SkillDamage damage) {
                metricsFor(initState, damage.sourceId(), firstTeam, secondTeam)
                    .ifPresent(metrics -> metrics.skillDamageDealt += damage.damageTaken());
                addCausal(
                    causalMetrics,
                    participants,
                    CausalMetricType.SKILL_DAMAGE,
                    damage.sourceId(),
                    damage.targetId(),
                    damage.skill(),
                    null,
                    damage.damageTaken()
                );
            } else if (event instanceof BattleEvent.EffectDamage damage) {
                metricsFor(initState, damage.sourceId(), firstTeam, secondTeam)
                    .ifPresent(metrics -> metrics.periodicDamageDealt += damage.damageTaken());
                addCausal(
                    causalMetrics,
                    participants,
                    CausalMetricType.PERIODIC_DAMAGE,
                    damage.sourceId(),
                    damage.targetId(),
                    damage.skill(),
                    null,
                    damage.damageTaken()
                );
            } else if (event instanceof BattleEvent.ScalingSkillDamage damage) {
                final var metrics = metricsFor(initState, damage.sourceId(), firstTeam, secondTeam);
                if (damage.periodic()) {
                    metrics.ifPresent(it -> it.periodicDamageDealt += damage.damageTaken());
                } else {
                    metrics.ifPresent(it -> it.skillDamageDealt += damage.damageTaken());
                }
                addCausal(
                    causalMetrics,
                    participants,
                    damage.periodic() ? CausalMetricType.PERIODIC_DAMAGE : CausalMetricType.SKILL_DAMAGE,
                    damage.sourceId(),
                    damage.targetId(),
                    damage.skill(),
                    null,
                    damage.damageTaken()
                );
            } else if (event instanceof BattleEvent.PersonageHealed healing) {
                metricsFor(initState, healing.personageId(), firstTeam, secondTeam)
                    .ifPresent(metrics -> metrics.healing += healing.amount());
                addCausal(
                    causalMetrics,
                    participants,
                    CausalMetricType.HEALING,
                    healing.personageId(),
                    healing.personageId(),
                    healing.skill(),
                    null,
                    healing.amount()
                );
            } else if (event instanceof BattleEvent.AttackIntercepted interception) {
                metricsFor(initState, interception.interceptorId(), firstTeam, secondTeam)
                    .ifPresent(metrics -> metrics.interceptions++);
                addCausal(
                    causalMetrics,
                    participants,
                    CausalMetricType.INTERCEPTION,
                    interception.interceptorId(),
                    interception.originalTargetId(),
                    ActiveEnum.GUARD,
                    null,
                    1
                );
            } else if (event instanceof BattleEvent.MovedTowardEnemy movement) {
                metricsFor(initState, movement.personageId(), firstTeam, secondTeam)
                    .ifPresent(metrics -> metrics.movements++);
                addCausal(
                    causalMetrics,
                    participants,
                    CausalMetricType.MOVEMENT,
                    movement.personageId(),
                    null,
                    null,
                    null,
                    1
                );
            } else if (event instanceof BattleEvent.PersonageForcedMove movement) {
                metricsFor(initState, movementSkillOwner(movement), firstTeam, secondTeam)
                    .ifPresent(metrics -> metrics.movements++);
                addCausal(
                    causalMetrics,
                    participants,
                    CausalMetricType.MOVEMENT,
                    movementSkillOwner(movement),
                    movement.personageId(),
                    movement.skill(),
                    null,
                    1
                );
            } else if (event instanceof BattleEvent.SkillChargeChanged charge) {
                if (charge.discharged()) {
                    metricsFor(initState, charge.personageId(), firstTeam, secondTeam)
                        .ifPresent(metrics -> metrics.discharges++);
                    addCausal(
                        causalMetrics,
                        participants,
                        CausalMetricType.DISCHARGE,
                        charge.personageId(),
                        charge.personageId(),
                        charge.skill(),
                        null,
                        1
                    );
                }
            } else if (event instanceof BattleEvent.SkillWindowUsed window) {
                metricsFor(initState, window.personageId(), firstTeam, secondTeam)
                    .ifPresent(metrics -> metrics.skillWindowsUsed++);
                addCausal(
                    causalMetrics,
                    participants,
                    CausalMetricType.SKILL_WINDOW,
                    window.personageId(),
                    null,
                    window.skill(),
                    null,
                    1
                );
            } else if (event instanceof BattleEvent.InitiativeDelayed delay) {
                metricsFor(initState, delay.sourceId(), firstTeam, secondTeam)
                    .ifPresent(metrics -> metrics.initiativeRemoved += delay.amount());
                addCausal(
                    causalMetrics,
                    participants,
                    CausalMetricType.INITIATIVE_REMOVED,
                    delay.sourceId(),
                    delay.targetId(),
                    delay.skill(),
                    null,
                    delay.amount()
                );
            } else if (event instanceof BattleEvent.TargetSelected selection) {
                metricsFor(initState, selection.attackerId(), firstTeam, secondTeam)
                    .ifPresent(metrics -> metrics.targetSelections++);
                addCausal(
                    causalMetrics,
                    participants,
                    CausalMetricType.TARGET_SELECTION,
                    selection.attackerId(),
                    selection.finalTargetId(),
                    null,
                    null,
                    1
                );
            } else if (event instanceof BattleEvent.ThreatChanged threat) {
                metricsFor(initState, threat.personageId(), firstTeam, secondTeam).ifPresent(metrics -> {
                    if (threat.delta() > 0) {
                        metrics.threatGained += threat.delta();
                    } else {
                        metrics.threatLost += -(long) threat.delta();
                    }
                    switch (threat.reason()) {
                        case NORMAL_HIT -> metrics.normalHitThreatDelta += threat.delta();
                        case KILL -> metrics.killThreatDelta += threat.delta();
                        case DAMAGE_TAKEN -> metrics.damageTakenThreatDelta += threat.delta();
                    }
                });
                addCausal(
                    causalMetrics,
                    participants,
                    CausalMetricType.THREAT_CHANGE,
                    threat.sourceId(),
                    threat.personageId(),
                    threat.skill(),
                    threat.reason(),
                    threat.delta()
                );
            }
        }
        if (!hasExplicitTargetSelections || !hasExplicitThreatChanges) {
            summarizeLegacyCausalEvents(
                initState,
                events,
                firstTeam,
                secondTeam,
                !hasExplicitTargetSelections,
                !hasExplicitThreatChanges,
                causalMetrics,
                participants
            );
        }
        return new EventSamples(firstTeam.snapshot(), secondTeam.snapshot(), Map.copyOf(causalMetrics));
    }

    private static void addCausal(
        Map<CausalMetricKey, Long> metrics,
        Map<UUID, ParticipantRef> participants,
        CausalMetricType type,
        UUID sourceId,
        UUID targetId,
        ActiveEnum skill,
        BattleEvent.ThreatReason threatReason,
        long value
    ) {
        if (sourceId == null) {
            return;
        }
        final var source = participants.get(sourceId);
        if (source == null || value == 0) {
            return;
        }
        final var target = targetId == null
            ? Optional.<ParticipantRef>empty()
            : Optional.ofNullable(participants.get(targetId));
        final var key = new CausalMetricKey(
            type,
            source,
            target,
            Optional.ofNullable(skill),
            Optional.ofNullable(threatReason)
        );
        metrics.merge(key, value, Math::addExact);
    }

    private static UUID movementSkillOwner(BattleEvent.PersonageForcedMove movement) {
        return movement.skill() == ActiveEnum.KNOCKBACK
            ? movement.sourceId()
            : movement.personageId();
    }

    /**
     * Legacy logs have no explicit target-selection or threat events. A normal-attempt gain is deferred until all
     * reactions are processed because reaction damage can reduce the attacker's existing bonus threat first.
     */
    private static void summarizeLegacyCausalEvents(
        BattleInitState initState,
        List<BattleEvent> events,
        MutableEventTeamSample firstTeam,
        MutableEventTeamSample secondTeam,
        boolean inferTargetSelections,
        boolean inferThreatChanges,
        Map<CausalMetricKey, Long> causalMetrics,
        Map<UUID, ParticipantRef> participants
    ) {
        final Map<UUID, Long> bonusThreat = new HashMap<>();
        LegacyAttempt pendingAttempt = null;
        for (final var event : events) {
            if (event instanceof BattleEvent.DamageReceived damage) {
                applyLegacyAttemptGain(
                    initState,
                    pendingAttempt,
                    bonusThreat,
                    firstTeam,
                    secondTeam,
                    inferThreatChanges,
                    causalMetrics,
                    participants
                );
                if (inferTargetSelections) {
                    metricsFor(initState, damage.attackerId(), firstTeam, secondTeam)
                        .ifPresent(metrics -> metrics.targetSelections++);
                    addCausal(
                        causalMetrics,
                        participants,
                        CausalMetricType.TARGET_SELECTION,
                        damage.attackerId(),
                        damage.targetId(),
                        null,
                        null,
                        1
                    );
                }
                if (inferThreatChanges) {
                    applyLegacyThreatLoss(
                        initState,
                        damage.targetId(),
                        damage.attackerId(),
                        null,
                        bonusThreat,
                        firstTeam,
                        secondTeam,
                        causalMetrics,
                        participants
                    );
                }
                pendingAttempt = new LegacyAttempt(
                    damage.attackerId(),
                    damage.targetId(),
                    true,
                    damage.remainingHealth() <= 0
                );
            } else if (event instanceof BattleEvent.AttackDodged dodge) {
                applyLegacyAttemptGain(
                    initState,
                    pendingAttempt,
                    bonusThreat,
                    firstTeam,
                    secondTeam,
                    inferThreatChanges,
                    causalMetrics,
                    participants
                );
                if (inferTargetSelections) {
                    metricsFor(initState, dodge.attackerId(), firstTeam, secondTeam)
                        .ifPresent(metrics -> metrics.targetSelections++);
                    addCausal(
                        causalMetrics,
                        participants,
                        CausalMetricType.TARGET_SELECTION,
                        dodge.attackerId(),
                        dodge.targetId(),
                        null,
                        null,
                        1
                    );
                }
                pendingAttempt = new LegacyAttempt(dodge.attackerId(), dodge.targetId(), false, false);
            } else if (event instanceof BattleEvent.EffectDamage damage) {
                applyLegacyAttemptGain(
                    initState,
                    pendingAttempt,
                    bonusThreat,
                    firstTeam,
                    secondTeam,
                    inferThreatChanges,
                    causalMetrics,
                    participants
                );
                pendingAttempt = null;
                if (inferThreatChanges) {
                    applyLegacyThreatLoss(
                        initState,
                        damage.targetId(),
                        damage.sourceId(),
                        damage.skill(),
                        bonusThreat,
                        firstTeam,
                        secondTeam,
                        causalMetrics,
                        participants
                    );
                }
            } else if (event instanceof BattleEvent.SkillDamage damage) {
                if (inferThreatChanges) {
                    applyLegacyThreatLoss(
                        initState,
                        damage.targetId(),
                        damage.sourceId(),
                        damage.skill(),
                        bonusThreat,
                        firstTeam,
                        secondTeam,
                        causalMetrics,
                        participants
                    );
                }
            } else if (event instanceof BattleEvent.PersonageDefeated defeated && pendingAttempt != null) {
                if (defeated.personageId().equals(pendingAttempt.targetId())
                    && defeated.killerId().equals(pendingAttempt.attackerId())) {
                    pendingAttempt = pendingAttempt.asKill();
                    applyLegacyAttemptGain(
                        initState,
                        pendingAttempt,
                        bonusThreat,
                        firstTeam,
                        secondTeam,
                        inferThreatChanges,
                        causalMetrics,
                        participants
                    );
                    pendingAttempt = null;
                } else if (defeated.personageId().equals(pendingAttempt.attackerId())) {
                    applyLegacyAttemptGain(
                        initState,
                        pendingAttempt,
                        bonusThreat,
                        firstTeam,
                        secondTeam,
                        inferThreatChanges,
                        causalMetrics,
                        participants
                    );
                    pendingAttempt = null;
                }
            } else if (legacyAttemptBoundary(event)) {
                applyLegacyAttemptGain(
                    initState,
                    pendingAttempt,
                    bonusThreat,
                    firstTeam,
                    secondTeam,
                    inferThreatChanges,
                    causalMetrics,
                    participants
                );
                pendingAttempt = null;
            }
        }
        applyLegacyAttemptGain(
            initState,
            pendingAttempt,
            bonusThreat,
            firstTeam,
            secondTeam,
            inferThreatChanges,
            causalMetrics,
            participants
        );
    }

    private static boolean legacyAttemptBoundary(BattleEvent event) {
        return event instanceof BattleEvent.RoundStarted
            || event instanceof BattleEvent.MovedTowardEnemy
            || event instanceof BattleEvent.PersonageHealed
            || event instanceof BattleEvent.PersonageAttackBuffed;
    }

    private static void applyLegacyAttemptGain(
        BattleInitState initState,
        LegacyAttempt attempt,
        Map<UUID, Long> bonusThreat,
        MutableEventTeamSample firstTeam,
        MutableEventTeamSample secondTeam,
        boolean inferThreatChanges,
        Map<CausalMetricKey, Long> causalMetrics,
        Map<UUID, ParticipantRef> participants
    ) {
        if (attempt == null || !inferThreatChanges) {
            return;
        }
        final int delta;
        final BattleEvent.ThreatReason reason;
        if (attempt.killed()) {
            delta = 50;
            reason = BattleEvent.ThreatReason.KILL;
        } else if (attempt.hit()) {
            delta = 5;
            reason = BattleEvent.ThreatReason.NORMAL_HIT;
        } else {
            return;
        }
        bonusThreat.merge(attempt.attackerId(), (long) delta, Math::addExact);
        metricsFor(initState, attempt.attackerId(), firstTeam, secondTeam).ifPresent(metrics -> {
            metrics.threatGained += delta;
            if (reason == BattleEvent.ThreatReason.KILL) {
                metrics.killThreatDelta += delta;
            } else {
                metrics.normalHitThreatDelta += delta;
            }
        });
        addCausal(
            causalMetrics,
            participants,
            CausalMetricType.THREAT_CHANGE,
            attempt.attackerId(),
            attempt.attackerId(),
            null,
            reason,
            delta
        );
    }

    private static void applyLegacyThreatLoss(
        BattleInitState initState,
        UUID targetId,
        UUID sourceId,
        ActiveEnum skill,
        Map<UUID, Long> bonusThreat,
        MutableEventTeamSample firstTeam,
        MutableEventTeamSample secondTeam,
        Map<CausalMetricKey, Long> causalMetrics,
        Map<UUID, ParticipantRef> participants
    ) {
        final long current = bonusThreat.getOrDefault(targetId, 0L);
        final long lost = Math.min(8L, current);
        bonusThreat.put(targetId, current - lost);
        if (lost == 0) {
            return;
        }
        metricsFor(initState, targetId, firstTeam, secondTeam).ifPresent(metrics -> {
            metrics.threatLost += lost;
            metrics.damageTakenThreatDelta -= lost;
        });
        addCausal(
            causalMetrics,
            participants,
            CausalMetricType.THREAT_CHANGE,
            sourceId,
            targetId,
            skill,
            BattleEvent.ThreatReason.DAMAGE_TAKEN,
            -lost
        );
    }

    private static Optional<MutableEventTeamSample> metricsFor(
        BattleInitState initState,
        UUID personageId,
        MutableEventTeamSample firstTeam,
        MutableEventTeamSample secondTeam
    ) {
        final var personage = initState.personagesById().get(personageId);
        if (personage == null) {
            return Optional.empty();
        }
        return Optional.of(personage.firstTeam() ? firstTeam : secondTeam);
    }

    static ConfidenceInterval wilson95(int wins, int iterations) {
        if (iterations <= 0 || wins < 0 || wins > iterations) {
            throw new IllegalArgumentException("Expected 0 <= wins <= iterations and iterations > 0");
        }
        return conservativeScore95(wins, iterations);
    }

    /**
     * Wilson score interval for a sum of independent observations in [0, 1]. For a fractional observation,
     * such as the score of two correlated battles sharing one seed, Bernoulli variance is the worst-case variance
     * because X² ≤ X. Using it therefore avoids claiming the fractional observations are independent Bernoulli trials.
     */
    static ConfidenceInterval conservativeScore95(double scoreSum, int samples) {
        if (samples <= 0 || !Double.isFinite(scoreSum) || scoreSum < 0 || scoreSum > samples) {
            throw new IllegalArgumentException("Expected 0 <= scoreSum <= samples and samples > 0");
        }
        final double proportion = scoreSum / samples;
        final double zSquared = Z_95 * Z_95;
        final double denominator = 1 + zSquared / samples;
        final double center = (proportion + zSquared / (2 * samples)) / denominator;
        final double margin = Z_95 * Math.sqrt(
            proportion * (1 - proportion) / samples + zSquared / (4 * samples * (double) samples)
        ) / denominator;
        return new ConfidenceInterval(Math.max(0, center - margin), Math.min(1, center + margin));
    }

    static double median(List<Integer> values) {
        final var sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
        if (sorted.isEmpty()) {
            throw new IllegalArgumentException("values must not be empty");
        }
        final int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    static int percentileNearestRank(List<Integer> values, double percentile) {
        if (values.isEmpty() || percentile <= 0 || percentile > 1) {
            throw new IllegalArgumentException("values must not be empty and percentile must be in (0, 1]");
        }
        final var sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
        final int index = Math.max(0, (int) Math.ceil(percentile * sorted.size()) - 1);
        return sorted.get(index);
    }

    static long iterationSeed(long seed, int iteration) {
        var mixed = seed + GOLDEN_GAMMA * (iteration + 1L);
        mixed = (mixed ^ mixed >>> 30) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ mixed >>> 27) * 0x94d049bb133111ebL;
        return mixed ^ mixed >>> 31;
    }

    private record BattleSample(
        boolean evaluatedTeamWin,
        int rounds,
        TeamSample evaluatedTeam,
        TeamSample opponents,
        List<StartingPosition> startingPositions,
        Map<CausalMetricKey, Long> causalMetrics
    ) {
    }

    private record TeamSample(
        int survivors,
        long remainingHealth,
        double remainingHealthPercent,
        long damageDealt,
        long damageTaken,
        long turns,
        EventTeamSample events
    ) {
        private TeamSample withDamageDealt(long value) {
            return new TeamSample(
                survivors,
                remainingHealth,
                remainingHealthPercent,
                value,
                damageTaken,
                turns,
                events
            );
        }

        private TeamSample withEvents(EventTeamSample value) {
            return new TeamSample(
                survivors,
                remainingHealth,
                remainingHealthPercent,
                damageDealt,
                damageTaken,
                turns,
                value
            );
        }
    }

    record EventSamples(
        EventTeamSample firstTeam,
        EventTeamSample secondTeam,
        Map<CausalMetricKey, Long> causalMetrics
    ) {
    }

    private record LegacyAttempt(UUID attackerId, UUID targetId, boolean hit, boolean killed) {
        private LegacyAttempt asKill() {
            return new LegacyAttempt(attackerId, targetId, hit, true);
        }
    }

    private record CausalMetricKey(
        CausalMetricType type,
        ParticipantRef source,
        Optional<ParticipantRef> target,
        Optional<ActiveEnum> skill,
        Optional<BattleEvent.ThreatReason> threatReason
    ) {
    }

    record EventTeamSample(
        long normalDamageDealt,
        long skillDamageDealt,
        long periodicDamageDealt,
        long healing,
        long interceptions,
        long movements,
        long discharges,
        long skillWindowsUsed,
        long initiativeRemoved,
        long targetSelections,
        long threatGained,
        long threatLost,
        long normalHitThreatDelta,
        long killThreatDelta,
        long damageTakenThreatDelta
    ) {
        private static final EventTeamSample EMPTY = new EventTeamSample(
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0
        );
    }

    private static final class MutableEventTeamSample {
        private long normalDamageDealt;
        private long skillDamageDealt;
        private long periodicDamageDealt;
        private long healing;
        private long interceptions;
        private long movements;
        private long discharges;
        private long skillWindowsUsed;
        private long initiativeRemoved;
        private long targetSelections;
        private long threatGained;
        private long threatLost;
        private long normalHitThreatDelta;
        private long killThreatDelta;
        private long damageTakenThreatDelta;

        private EventTeamSample snapshot() {
            return new EventTeamSample(
                normalDamageDealt,
                skillDamageDealt,
                periodicDamageDealt,
                healing,
                interceptions,
                movements,
                discharges,
                skillWindowsUsed,
                initiativeRemoved,
                targetSelections,
                threatGained,
                threatLost,
                normalHitThreatDelta,
                killThreatDelta,
                damageTakenThreatDelta
            );
        }
    }

    private static final class MutableTeamTotals {
        private long survivors;
        private long remainingHealth;
        private double remainingHealthPercent;
        private long damageDealt;
        private long damageTaken;
        private long turns;
        private long normalDamageDealt;
        private long skillDamageDealt;
        private long periodicDamageDealt;
        private long healing;
        private long interceptions;
        private long movements;
        private long discharges;
        private long skillWindowsUsed;
        private long initiativeRemoved;
        private long targetSelections;
        private long threatGained;
        private long threatLost;
        private long normalHitThreatDelta;
        private long killThreatDelta;
        private long damageTakenThreatDelta;

        private void add(TeamSample sample) {
            survivors += sample.survivors();
            remainingHealth += sample.remainingHealth();
            remainingHealthPercent += sample.remainingHealthPercent();
            damageDealt += sample.damageDealt();
            damageTaken += sample.damageTaken();
            turns += sample.turns();
            normalDamageDealt += sample.events().normalDamageDealt();
            skillDamageDealt += sample.events().skillDamageDealt();
            periodicDamageDealt += sample.events().periodicDamageDealt();
            healing += sample.events().healing();
            interceptions += sample.events().interceptions();
            movements += sample.events().movements();
            discharges += sample.events().discharges();
            skillWindowsUsed += sample.events().skillWindowsUsed();
            initiativeRemoved += sample.events().initiativeRemoved();
            targetSelections += sample.events().targetSelections();
            threatGained += sample.events().threatGained();
            threatLost += sample.events().threatLost();
            normalHitThreatDelta += sample.events().normalHitThreatDelta();
            killThreatDelta += sample.events().killThreatDelta();
            damageTakenThreatDelta += sample.events().damageTakenThreatDelta();
        }

        private TeamMetrics average(int iterations) {
            return new TeamMetrics(
                (double) survivors / iterations,
                (double) remainingHealth / iterations,
                remainingHealthPercent / iterations,
                (double) damageDealt / iterations,
                (double) damageTaken / iterations,
                (double) turns / iterations,
                (double) normalDamageDealt / iterations,
                (double) skillDamageDealt / iterations,
                (double) periodicDamageDealt / iterations,
                (double) healing / iterations,
                (double) interceptions / iterations,
                (double) movements / iterations,
                (double) discharges / iterations,
                (double) skillWindowsUsed / iterations,
                (double) initiativeRemoved / iterations,
                (double) targetSelections / iterations,
                (double) threatGained / iterations,
                (double) threatLost / iterations,
                (double) normalHitThreatDelta / iterations,
                (double) killThreatDelta / iterations,
                (double) damageTakenThreatDelta / iterations
            );
        }
    }

    private static final class MutableCausalTotals {
        private final Map<CausalMetricKey, Long> totals = new HashMap<>();

        private void add(Map<CausalMetricKey, Long> sample) {
            sample.forEach((key, value) -> totals.merge(key, value, Math::addExact));
        }

        private List<CausalMetric> average(int iterations) {
            return totals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(CAUSAL_METRIC_ORDER))
                .map(entry -> new CausalMetric(
                    entry.getKey().type(),
                    entry.getKey().source(),
                    entry.getKey().target(),
                    entry.getKey().skill(),
                    entry.getKey().threatReason(),
                    (double) entry.getValue() / iterations
                ))
                .toList();
        }
    }
}
