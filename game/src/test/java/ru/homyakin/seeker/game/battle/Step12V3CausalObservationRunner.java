package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import ru.homyakin.seeker.game.battle.simulation.CombatSimulationTeams;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V3Build;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V3Matchup;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.AttackAccess;
import ru.homyakin.seeker.utils.RandomUtils;

/** Collapses revision-three battles to the observations used by the repeatable causal diagnosis. */
final class Step12V3CausalObservationRunner {
    private static final long GOLDEN_GAMMA = 0x9e3779b97f4a7c15L;
    private static final int PENETRATION_DENOMINATOR = 12;

    private Step12V3CausalObservationRunner() {
    }

    static NaturalPairObservation naturalPair(
        V3Matchup matchup,
        int enhanceLevel,
        long cellSeed,
        int iteration,
        int maxRounds
    ) {
        final long seed = iterationSeed(cellSeed, iteration);
        final var first = naturalBattle(matchup, enhanceLevel, seed, false, Set.of(), maxRounds);
        final var second = naturalBattle(matchup, enhanceLevel, seed, true, Set.of(), maxRounds);
        return new NaturalPairObservation(
            first.bruiserDamage() + second.bruiserDamage(),
            first.bruiserAttempts() + second.bruiserAttempts(),
            first.guardianDamage() + second.guardianDamage(),
            first.guardianAttempts() + second.guardianAttempts(),
            (first.rangerFarAttemptShare() + second.rangerFarAttemptShare()) / 2,
            pairScore(first.breakerDischarged(), second.breakerDischarged()),
            pairScore(first.tacticianApplied(), second.tacticianApplied()),
            pairScore(first.assassinReachedRanger(), second.assassinReachedRanger())
        );
    }

    static double skirmisherPair(
        int enhanceLevel,
        long cellSeed,
        int iteration,
        int maxRounds
    ) {
        final long seed = iterationSeed(cellSeed, iteration);
        return pairScore(
            skirmisherSide(enhanceLevel, seed, false, maxRounds),
            skirmisherSide(enhanceLevel, seed, true, maxRounds)
        );
    }

    static double assassinPair(int enhanceLevel, long cellSeed, int iteration) {
        final long seed = iterationSeed(cellSeed, iteration);
        return pairScore(
            assassinSide(enhanceLevel, seed, false),
            assassinSide(enhanceLevel, seed, true)
        );
    }

    static GuardianPairObservation guardianPair(
        int partySize,
        int enhanceLevel,
        long cellSeed,
        int iteration,
        int maxRounds
    ) {
        final long seed = iterationSeed(cellSeed, iteration);
        final var first = guardianSide(partySize, enhanceLevel, seed, false, maxRounds);
        final var second = guardianSide(partySize, enhanceLevel, seed, true, maxRounds);
        return new GuardianPairObservation(
            pairScore(first.usefulInterception(), second.usefulInterception()),
            (first.controlWardDamage() + second.controlWardDamage()
                - first.activeWardDamage() - second.activeWardDamage()) / 2.0,
            (first.activeWardTurns() + second.activeWardTurns()
                - first.controlWardTurns() - second.controlWardTurns()) / 2.0,
            first.interceptionLimitKept() && second.interceptionLimitKept()
        );
    }

    static TempoExactObservation tempoExact(int enhanceLevel, long cellSeed) {
        final var first = tempoExactSide(enhanceLevel, cellSeed, false);
        final var second = tempoExactSide(enhanceLevel, cellSeed, true);
        return new TempoExactObservation(
            first.impactStrength(),
            second.impactStrength(),
            first.removed(),
            second.removed()
        );
    }

    static TempoBenchmarkObservation tempoBenchmark(
        int enhanceLevel,
        long cellSeed,
        int warmupSteps,
        int measuredSteps
    ) {
        if (warmupSteps < 0 || measuredSteps <= 0) {
            throw new IllegalArgumentException("Tempo benchmark steps must be non-negative and positive");
        }
        final long activeFirst = tempoSide(
            enhanceLevel,
            cellSeed,
            false,
            false,
            warmupSteps,
            measuredSteps
        );
        final long controlFirst = tempoSide(
            enhanceLevel,
            cellSeed,
            false,
            true,
            warmupSteps,
            measuredSteps
        );
        final long activeSecond = tempoSide(
            enhanceLevel,
            cellSeed,
            true,
            false,
            warmupSteps,
            measuredSteps
        );
        final long controlSecond = tempoSide(
            enhanceLevel,
            cellSeed,
            true,
            true,
            warmupSteps,
            measuredSteps
        );
        final long activeTurns = activeFirst + activeSecond;
        final long controlTurns = controlFirst + controlSecond;
        if (controlTurns <= 0) {
            throw new IllegalStateException("Tempo benchmark control produced no Bruiser turns");
        }
        return new TempoBenchmarkObservation(
            activeTurns,
            controlTurns,
            (double) (controlTurns - activeTurns) / controlTurns
        );
    }

    static long iterationSeed(long seed, int iteration) {
        var mixed = seed + GOLDEN_GAMMA * (iteration + 1L);
        mixed = (mixed ^ mixed >>> 30) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ mixed >>> 27) * 0x94d049bb133111ebL;
        return mixed ^ mixed >>> 31;
    }

    private static TempoSideObservation tempoExactSide(int enhanceLevel, long seed, boolean reverse) {
        return RandomUtils.withSeed(seed, () -> {
            final var tactician = personage(V3Build.TACTICIAN_MAGICAL, Position.FRONT, enhanceLevel);
            final var bruiser = personage(V3Build.BRUISER_MAGICAL_CLOTH, Position.FRONT, enhanceLevel);
            bruiser.setInitiativeGaugeForTest(500);
            final BattleRandom random = new DiagnosticBattleRandom(seed, bruiser.id());
            final var context = reverse
                ? new BattleContext(List.of(bruiser), List.of(tactician), random)
                : new BattleContext(List.of(tactician), List.of(bruiser), random);
            final var log = new BattleActionLog();
            tactician.move(context, log, 1);
            final var delay = events(log, BattleEvent.InitiativeDelayed.class).stream()
                .filter(event -> event.sourceId().equals(tactician.id()))
                .filter(event -> event.targetId().equals(bruiser.id()))
                .findFirst()
                .orElseThrow();
            return new TempoSideObservation(tactician.impactStrength(), delay.amount());
        });
    }

    private static long tempoSide(
        int enhanceLevel,
        long seed,
        boolean reverse,
        boolean withoutTempoBreak,
        int warmupSteps,
        int measuredSteps
    ) {
        return RandomUtils.withSeed(seed, () -> {
            final var tactician = withoutTempoBreak
                ? Step12SimulationFixtures.v3PersonageWithoutModifier(
                    V3Build.TACTICIAN_MAGICAL,
                    Position.FRONT,
                    enhanceLevel
                )
                : personage(V3Build.TACTICIAN_MAGICAL, Position.FRONT, enhanceLevel);
            final var bruiser = personage(V3Build.BRUISER_MAGICAL_CLOTH, Position.FRONT, enhanceLevel);
            final BattleRandom random = new DiagnosticBattleRandom(seed, bruiser.id());
            final var context = reverse
                ? new BattleContext(List.of(bruiser), List.of(tactician), random)
                : new BattleContext(List.of(tactician), List.of(bruiser), random);
            advanceTempoBenchmark(context, random, tactician, bruiser, reverse, 1, warmupSteps);
            final int turnsBefore = bruiser.battlePersonageStats().turnsCount();
            advanceTempoBenchmark(
                context,
                random,
                tactician,
                bruiser,
                reverse,
                warmupSteps + 1,
                measuredSteps
            );
            return (long) bruiser.battlePersonageStats().turnsCount() - turnsBefore;
        });
    }

    private static void advanceTempoBenchmark(
        BattleContext context,
        BattleRandom random,
        BattlePersonage tactician,
        BattlePersonage bruiser,
        boolean reverse,
        int firstStep,
        int steps
    ) {
        for (int offset = 0; offset < steps; offset++) {
            final int step = Math.addExact(firstStep, offset);
            final var log = new BattleActionLog();
            final var movers = new ArrayList<BattlePersonage>(2);
            final var tickOrder = reverse ? List.of(bruiser, tactician) : List.of(tactician, bruiser);
            for (final var personage : tickOrder) {
                if (personage.tick(log, step)) {
                    movers.add(personage);
                }
            }
            for (final var mover : random.shuffle("turn-order:" + step, movers)) {
                mover.move(context, log, step);
                if (!tactician.isAlive() || !bruiser.isAlive()) {
                    throw new IllegalStateException("Tempo benchmark must not contain a defeat");
                }
                tactician.heal(tactician.maxHealth());
                bruiser.heal(bruiser.maxHealth());
            }
        }
    }

    private static NaturalBattleObservation naturalBattle(
        V3Matchup matchup,
        int enhanceLevel,
        long seed,
        boolean reverse,
        Set<V3Build> disabled,
        int maxRounds
    ) {
        return RandomUtils.withSeed(seed, () -> {
            final var teams = Step12SimulationFixtures.v3Teams(matchup, enhanceLevel, disabled);
            final var result = process(new Battle(seed), teams, reverse, maxRounds);
            final var traces = result.actionLog().traceEvents();
            final var events = result.actionLog().events();
            final var pair = rolePair(matchup);
            final var evaluated = named(teams.evaluatedTeam(), pair.evaluated());
            final var opponent = named(teams.opponents(), pair.opponent());
            final long evaluatedDamage = damageBy(traces, evaluated.id());
            final long evaluatedAttempts = attemptsBy(traces, evaluated.id()).size();
            final long opponentDamage = damageBy(traces, opponent.id());
            final long opponentAttempts = attemptsBy(traces, opponent.id()).size();
            final var evaluatedAttemptsList = attemptsBy(traces, evaluated.id());
            final double farShare = evaluatedAttemptsList.isEmpty()
                ? 0
                : (double) evaluatedAttemptsList.stream()
                    .filter(attempt -> attempt.distance() >= 3 && attempt.distance() <= 4)
                    .count() / evaluatedAttemptsList.size();
            final boolean discharged = evaluatedAttemptsList.stream()
                .anyMatch(BattleTraceEvent.NormalAttackAttempt::discharge);
            final boolean tempoApplied = events.stream()
                .filter(BattleEvent.InitiativeDelayed.class::isInstance)
                .map(BattleEvent.InitiativeDelayed.class::cast)
                .anyMatch(event -> event.sourceId().equals(evaluated.id())
                    && event.targetId().equals(opponent.id())
                    && event.skill() == ActiveEnum.TEMPO_BREAK);
            final boolean assassinAccess = evaluatedAttemptsList.stream()
                .anyMatch(attempt -> attempt.finalTargetId().equals(opponent.id())
                    && attempt.access() == AttackAccess.PENETRATION);
            return switch (matchup) {
                case BRUISER_V3 -> new NaturalBattleObservation(
                    evaluatedDamage,
                    evaluatedAttempts,
                    opponentDamage,
                    opponentAttempts,
                    0,
                    false,
                    false,
                    false
                );
                case RANGER_V3 -> NaturalBattleObservation.withRangerShare(farShare);
                case BREAKER_V3 -> NaturalBattleObservation.withBreakerDischarge(discharged);
                case TACTICIAN_V3 -> NaturalBattleObservation.withTacticianApplication(tempoApplied);
                case ASSASSIN_V3 -> NaturalBattleObservation.withAssassinAccess(assassinAccess);
                case SKIRMISHER_V3 -> NaturalBattleObservation.EMPTY;
            };
        });
    }

    static boolean skirmisherSide(int enhanceLevel, long seed, boolean reverse, int maxRounds) {
        final var active = skirmisherBattle(enhanceLevel, seed, reverse, Set.of(), maxRounds);
        final var control = skirmisherBattle(
            enhanceLevel,
            seed,
            reverse,
            Set.of(V3Build.SKIRMISHER_SLASH),
            maxRounds
        );
        final var activeWindow = firstAttemptBy(active.result().actionLog().traceEvents(), active.skirmisherId());
        if (activeWindow.isEmpty()
            || activeWindow.get().access() != AttackAccess.HIT_AND_RUN
            || !Step12CausalObservationRunner.retreatedBackward(
                activeWindow.get(),
                active.skirmisherDirection()
            )) {
            return false;
        }
        final var activePursuit = nextPerformedTurn(
            active.result().actionLog().traceEvents(),
            active.breakerId(),
            activeWindow.get().turnId()
        );
        if (activePursuit.isEmpty()
            || hasAttempt(
                active.result().actionLog().traceEvents(),
                active.breakerId(),
                activePursuit.get().turnId()
            )
            || gainedAccumulationCharge(
                active.result().actionLog(),
                active.breakerId(),
                activePursuit.get().round()
            )) {
            return false;
        }

        final var controlOpening = firstAttemptBy(
            control.result().actionLog().traceEvents(),
            control.skirmisherId()
        );
        if (controlOpening.isEmpty()) {
            return false;
        }
        final var controlPursuit = nextPerformedTurn(
            control.result().actionLog().traceEvents(),
            control.breakerId(),
            controlOpening.get().turnId()
        );
        if (controlPursuit.isEmpty()) {
            return false;
        }
        return attemptsBy(control.result().actionLog().traceEvents(), control.breakerId()).stream()
            .filter(attempt -> attempt.turnId() == controlPursuit.get().turnId())
            .anyMatch(attempt -> attempt.accumulationChargesAfter()
                == attempt.accumulationChargesBefore() + 1);
    }

    private static SkirmisherBattle skirmisherBattle(
        int enhanceLevel,
        long seed,
        boolean reverse,
        Set<V3Build> disabled,
        int maxRounds
    ) {
        return RandomUtils.withSeed(seed, () -> {
            final var teams = Step12SimulationFixtures.v3Teams(
                V3Matchup.SKIRMISHER_V3,
                enhanceLevel,
                disabled
            );
            final var skirmisher = named(teams.evaluatedTeam(), V3Build.SKIRMISHER_SLASH);
            final var breaker = named(teams.opponents(), V3Build.BREAKER_CLOSE_SLASH);
            final var result = process(new Battle(seed), teams, reverse, maxRounds);
            return new SkirmisherBattle(
                result,
                skirmisher.id(),
                breaker.id(),
                skirmisher.advanceDirection()
            );
        });
    }

    private static boolean assassinSide(int enhanceLevel, long seed, boolean reverse) {
        final var active = controlledAssassin(enhanceLevel, seed, reverse, false);
        final var control = controlledAssassin(enhanceLevel, seed, reverse, true);
        final var attempts = attemptsBy(active.log().traceEvents(), active.assassinId());
        final var packets = events(active.log(), BattleEvent.ScalingSkillDamage.class).stream()
            .filter(event -> event.sourceId().equals(active.assassinId()))
            .filter(event -> event.skill() == ActiveEnum.PENETRATION)
            .toList();
        final var windows = events(active.log(), BattleEvent.SkillWindowUsed.class).stream()
            .filter(event -> event.personageId().equals(active.assassinId()))
            .filter(event -> event.skill() == ActiveEnum.PENETRATION)
            .toList();
        final boolean threeLockedAttempts = attempts.size() == 3
            && attempts.stream().allMatch(attempt -> attempt.originalTargetId().equals(active.rangerId()))
            && attempts.stream().allMatch(attempt -> attempt.finalTargetId().equals(active.rangerId()))
            && attempts.stream().allMatch(attempt -> attempt.access() == AttackAccess.PENETRATION);
        final boolean oneOpeningPacket = packets.size() == 1
            && windows.size() == 1
            && packets.getFirst().targetId().equals(active.rangerId())
            && packets.getFirst().basis().equals(attempts.getFirst().savedBasis())
            && packets.getFirst().coefficientDenominator() == PENETRATION_DENOMINATOR
            && packets.getFirst().damageTaken() > 0;
        final boolean controlDamageIsZero = attemptsBy(control.log().traceEvents(), control.assassinId()).stream()
            .filter(attempt -> attempt.finalTargetId().equals(control.rangerId()))
            .mapToLong(BattleTraceEvent.NormalAttackAttempt::normalDamage)
            .sum() == 0;
        return threeLockedAttempts
            && oneOpeningPacket
            && active.lockCleared()
            && controlDamageIsZero;
    }

    private static ControlledAssassin controlledAssassin(
        int enhanceLevel,
        long seed,
        boolean reverse,
        boolean withoutSkill
    ) {
        return RandomUtils.withSeed(seed, () -> {
            final var disabled = withoutSkill
                ? Set.of(V3Build.ASSASSIN_PIERCE)
                : Set.<V3Build>of();
            final var teams = Step12SimulationFixtures.v3Teams(
                V3Matchup.ASSASSIN_V3,
                enhanceLevel,
                disabled
            );
            final var assassin = named(teams.evaluatedTeam(), V3Build.ASSASSIN_PIERCE);
            final var ranger = named(teams.opponents(), V3Build.RANGER_PIERCE);
            final var random = new DiagnosticBattleRandom(seed, ranger.id());
            final var context = reverse
                ? new BattleContext(teams.opponents(), teams.evaluatedTeam(), random)
                : new BattleContext(teams.evaluatedTeam(), teams.opponents(), random);
            final var log = new BattleActionLog();
            final int attempts = withoutSkill ? 1 : 3;
            for (int round = 1; round <= attempts; round++) {
                assassin.move(context, log, round);
                if (ranger.isAlive()) {
                    ranger.heal(ranger.maxHealth());
                }
            }
            return new ControlledAssassin(
                log,
                assassin.id(),
                ranger.id(),
                assassin.scalingSkills().penetrationTargetId().isEmpty()
            );
        });
    }

    private static GuardianSideObservation guardianSide(
        int partySize,
        int enhanceLevel,
        long seed,
        boolean reverse,
        int maxRounds
    ) {
        final var active = guardianBattle(partySize, enhanceLevel, seed, reverse, false, maxRounds);
        final var control = guardianBattle(partySize, enhanceLevel, seed, reverse, true, maxRounds);
        final var activeAttempts = attemptsBy(active.result().actionLog().traceEvents(), null);
        final var controlAttempts = attemptsBy(control.result().actionLog().traceEvents(), null);
        final boolean useful = Step12CausalObservationRunner.usefulGuardianOpportunity(
            activeAttempts,
            controlAttempts,
            active.wardId(),
            active.guardianId(),
            control.wardId()
        );
        final long activeWardDamage = damageTo(activeAttempts, active.wardId());
        final long controlWardDamage = damageTo(controlAttempts, control.wardId());
        final long activeWardTurns = performedActionsBy(
            active.result().actionLog().traceEvents(),
            active.wardId()
        );
        final long controlWardTurns = performedActionsBy(
            control.result().actionLog().traceEvents(),
            control.wardId()
        );
        final long interceptions = active.result().actionLog().events().stream()
            .filter(BattleEvent.AttackIntercepted.class::isInstance)
            .count();
        final long enemyAttempts = activeAttempts.stream()
            .filter(attempt -> active.enemyIds().contains(attempt.attackerId()))
            .count();
        return new GuardianSideObservation(
            useful,
            activeWardDamage,
            controlWardDamage,
            activeWardTurns,
            controlWardTurns,
            interceptions <= Math.ceilDiv(enemyAttempts, 4)
        );
    }

    private static GuardianBattle guardianBattle(
        int partySize,
        int enhanceLevel,
        long seed,
        boolean reverse,
        boolean withoutGuard,
        int maxRounds
    ) {
        return RandomUtils.withSeed(seed, () -> {
            final var teams = guardianTeams(partySize, enhanceLevel, withoutGuard);
            final var guardian = named(teams.evaluatedTeam(), V3Build.GUARDIAN_BLUNT);
            final var ward = teams.evaluatedTeam().stream()
                .filter(personage -> personage.name().orElseThrow().equals(V3Build.SUPPORT.displayName()))
                .filter(personage -> personage.startPosition() == Position.MID)
                .sorted(Comparator.comparing(BattlePersonage::id))
                .findFirst()
                .orElseThrow();
            final var enemyIds = teams.opponents().stream()
                .map(BattlePersonage::id)
                .collect(java.util.stream.Collectors.toSet());
            final var random = new Step12OneShotBattleRandom(seed, ward.id());
            final var result = process(new Battle(random), teams, reverse, maxRounds);
            return new GuardianBattle(result, guardian.id(), ward.id(), enemyIds);
        });
    }

    private static CombatSimulationTeams guardianTeams(
        int partySize,
        int enhanceLevel,
        boolean withoutGuard
    ) {
        final Supplier<BattlePersonage> guardian = () -> withoutGuard
            ? Step12SimulationFixtures.v3PersonageWithoutModifier(
                V3Build.GUARDIAN_BLUNT,
                Position.FRONT,
                enhanceLevel
            )
            : personage(V3Build.GUARDIAN_BLUNT, Position.FRONT, enhanceLevel);
        return switch (partySize) {
            case 3 -> new CombatSimulationTeams(
                List.of(
                    guardian.get(),
                    personage(V3Build.SUPPORT, Position.FRONT, enhanceLevel),
                    personage(V3Build.SUPPORT, Position.MID, enhanceLevel)
                ),
                List.of(
                    personage(V3Build.BRUISER_BLUNT, Position.FRONT, enhanceLevel),
                    personage(V3Build.SUPPORT, Position.FRONT, enhanceLevel),
                    personage(V3Build.SUPPORT, Position.MID, enhanceLevel)
                )
            );
            case 7 -> new CombatSimulationTeams(
                List.of(
                    guardian.get(),
                    personage(V3Build.SUPPORT, Position.FRONT, enhanceLevel),
                    personage(V3Build.SUPPORT, Position.FRONT, enhanceLevel),
                    personage(V3Build.SUPPORT, Position.FRONT, enhanceLevel),
                    personage(V3Build.SUPPORT, Position.MID, enhanceLevel),
                    personage(V3Build.SUPPORT, Position.MID, enhanceLevel),
                    personage(V3Build.SUPPORT, Position.MID, enhanceLevel)
                ),
                List.of(
                    personage(V3Build.BRUISER_BLUNT, Position.FRONT, enhanceLevel),
                    personage(V3Build.BRUISER_BLUNT, Position.FRONT, enhanceLevel),
                    personage(V3Build.BRUISER_BLUNT, Position.FRONT, enhanceLevel),
                    personage(V3Build.SUPPORT, Position.FRONT, enhanceLevel),
                    personage(V3Build.SUPPORT, Position.FRONT, enhanceLevel),
                    personage(V3Build.SUPPORT, Position.MID, enhanceLevel),
                    personage(V3Build.SUPPORT, Position.MID, enhanceLevel)
                )
            );
            default -> throw new IllegalArgumentException("Guardian party size must be 3 or 7");
        };
    }

    private static BattlePersonage personage(V3Build build, Position position, int enhanceLevel) {
        return Step12SimulationFixtures.v3Personage(build, position, enhanceLevel);
    }

    private static BattleResult process(
        Battle battle,
        CombatSimulationTeams teams,
        boolean reverse,
        int maxRounds
    ) {
        return reverse
            ? battle.process(teams.opponents(), teams.evaluatedTeam(), maxRounds)
            : battle.process(teams.evaluatedTeam(), teams.opponents(), maxRounds);
    }

    private static V3RolePair rolePair(V3Matchup matchup) {
        return switch (matchup) {
            case BRUISER_V3 -> new V3RolePair(V3Build.BRUISER_BLUNT, V3Build.GUARDIAN_BLUNT);
            case SKIRMISHER_V3 -> new V3RolePair(V3Build.SKIRMISHER_SLASH, V3Build.BREAKER_CLOSE_SLASH);
            case ASSASSIN_V3 -> new V3RolePair(V3Build.ASSASSIN_PIERCE, V3Build.RANGER_PIERCE);
            case RANGER_V3 -> new V3RolePair(V3Build.RANGER_MAGICAL, V3Build.BRUISER_MAGICAL_CLOTH);
            case BREAKER_V3 -> new V3RolePair(V3Build.BREAKER_MAGICAL_LEATHER, V3Build.BRUISER_MAGICAL_LEATHER);
            case TACTICIAN_V3 -> new V3RolePair(V3Build.TACTICIAN_MAGICAL, V3Build.BRUISER_MAGICAL_CLOTH);
        };
    }

    private static BattlePersonage named(List<BattlePersonage> team, V3Build build) {
        return team.stream()
            .filter(personage -> personage.name().orElseThrow().equals(build.displayName()))
            .findFirst()
            .orElseThrow();
    }

    private static List<BattleTraceEvent.NormalAttackAttempt> attemptsBy(
        List<BattleTraceEvent> traces,
        UUID attackerId
    ) {
        return traces.stream()
            .filter(BattleTraceEvent.NormalAttackAttempt.class::isInstance)
            .map(BattleTraceEvent.NormalAttackAttempt.class::cast)
            .filter(attempt -> attackerId == null || attempt.attackerId().equals(attackerId))
            .toList();
    }

    private static long damageBy(List<BattleTraceEvent> traces, UUID attackerId) {
        return attemptsBy(traces, attackerId).stream()
            .mapToLong(attempt -> (long) attempt.normalDamage() + attempt.dischargeDamage())
            .sum();
    }

    private static long damageTo(List<BattleTraceEvent.NormalAttackAttempt> attempts, UUID targetId) {
        return attempts.stream()
            .filter(attempt -> attempt.finalTargetId().equals(targetId))
            .mapToLong(BattleTraceEvent.NormalAttackAttempt::normalDamage)
            .sum();
    }

    static long performedActionsBy(List<BattleTraceEvent> traces, UUID personageId) {
        return traces.stream()
            .filter(BattleTraceEvent.TurnFinished.class::isInstance)
            .map(BattleTraceEvent.TurnFinished.class::cast)
            .filter(turn -> turn.personageId().equals(personageId))
            .filter(BattleTraceEvent.TurnFinished::actionPerformed)
            .count();
    }

    private static Optional<BattleTraceEvent.NormalAttackAttempt> firstAttemptBy(
        List<BattleTraceEvent> traces,
        UUID attackerId
    ) {
        return attemptsBy(traces, attackerId).stream().findFirst();
    }

    private static boolean gainedAccumulationCharge(BattleActionLog log, UUID personageId, int round) {
        return events(log, BattleEvent.SkillChargeChanged.class).stream()
            .anyMatch(event -> event.personageId().equals(personageId)
                && event.skill() == ActiveEnum.ACCUMULATION
                && event.round() == round
                && !event.discharged());
    }

    static Optional<BattleTraceEvent.TurnFinished> nextPerformedTurn(
        List<BattleTraceEvent> traces,
        UUID personageId,
        long afterTurnId
    ) {
        return traces.stream()
            .filter(BattleTraceEvent.TurnFinished.class::isInstance)
            .map(BattleTraceEvent.TurnFinished.class::cast)
            .filter(turn -> turn.personageId().equals(personageId) && turn.turnId() > afterTurnId)
            .filter(BattleTraceEvent.TurnFinished::actionPerformed)
            .findFirst();
    }

    private static boolean hasAttempt(List<BattleTraceEvent> traces, UUID attackerId, long turnId) {
        return attemptsBy(traces, attackerId).stream().anyMatch(attempt -> attempt.turnId() == turnId);
    }

    private static <T extends BattleEvent> List<T> events(BattleActionLog log, Class<T> type) {
        return log.events().stream()
            .filter(type::isInstance)
            .map(type::cast)
            .toList();
    }

    private static double pairScore(boolean first, boolean second) {
        return ((first ? 1 : 0) + (second ? 1 : 0)) / 2.0;
    }

    record NaturalPairObservation(
        double bruiserDamage,
        double bruiserAttempts,
        double guardianDamage,
        double guardianAttempts,
        double rangerFarAttemptShare,
        double breakerDischargeScore,
        double tacticianApplicationScore,
        double assassinNaturalAccessScore
    ) {
    }

    record GuardianPairObservation(
        double usefulInterceptionScore,
        double lifetimeWardDamageReduction,
        double wardTurnIncrease,
        boolean interceptionLimitKept
    ) {
    }

    record TempoExactObservation(
        int firstImpactStrength,
        int secondImpactStrength,
        int firstRemoved,
        int secondRemoved
    ) {
        boolean accepted() {
            return firstImpactStrength == 92
                && secondImpactStrength == 92
                && firstRemoved == 331
                && secondRemoved == 331;
        }
    }

    record TempoBenchmarkObservation(long activeBruiserTurns, long controlBruiserTurns, double relativeLoss) {
    }

    private record NaturalBattleObservation(
        long bruiserDamage,
        long bruiserAttempts,
        long guardianDamage,
        long guardianAttempts,
        double rangerFarAttemptShare,
        boolean breakerDischarged,
        boolean tacticianApplied,
        boolean assassinReachedRanger
    ) {
        private static final NaturalBattleObservation EMPTY = new NaturalBattleObservation(
            0, 0, 0, 0, 0, false, false, false
        );

        private static NaturalBattleObservation withRangerShare(double value) {
            return new NaturalBattleObservation(0, 0, 0, 0, value, false, false, false);
        }

        private static NaturalBattleObservation withBreakerDischarge(boolean value) {
            return new NaturalBattleObservation(0, 0, 0, 0, 0, value, false, false);
        }

        private static NaturalBattleObservation withTacticianApplication(boolean value) {
            return new NaturalBattleObservation(0, 0, 0, 0, 0, false, value, false);
        }

        private static NaturalBattleObservation withAssassinAccess(boolean value) {
            return new NaturalBattleObservation(0, 0, 0, 0, 0, false, false, value);
        }
    }

    private record TempoSideObservation(int impactStrength, int removed) {
    }

    private record SkirmisherBattle(
        BattleResult result,
        UUID skirmisherId,
        UUID breakerId,
        BattleAdvanceDirection skirmisherDirection
    ) {
    }

    private record ControlledAssassin(
        BattleActionLog log,
        UUID assassinId,
        UUID rangerId,
        boolean lockCleared
    ) {
    }

    private record GuardianBattle(
        BattleResult result,
        UUID guardianId,
        UUID wardId,
        Set<UUID> enemyIds
    ) {
    }

    private record GuardianSideObservation(
        boolean usefulInterception,
        long activeWardDamage,
        long controlWardDamage,
        long activeWardTurns,
        long controlWardTurns,
        boolean interceptionLimitKept
    ) {
    }

    private record V3RolePair(V3Build evaluated, V3Build opponent) {
    }

    /** Prevents criticals and dodges while retaining deterministic target, damage and initiative streams. */
    private static final class DiagnosticBattleRandom implements BattleRandom {
        private final SeededBattleRandom delegate;
        private final UUID forcedTargetId;
        private boolean targetForced;

        private DiagnosticBattleRandom(long seed, UUID forcedTargetId) {
            delegate = new SeededBattleRandom(seed);
            this.forcedTargetId = forcedTargetId;
        }

        @Override
        public int nextInt(String sequence, int minimumInclusive, int maximumInclusive) {
            final int delegated = delegate.nextInt(sequence, minimumInclusive, maximumInclusive);
            if (sequence.startsWith("normal-critical:") || sequence.startsWith("normal-dodge:")) {
                return maximumInclusive;
            }
            if (sequence.startsWith("normal-damage:") || sequence.startsWith("skill-damage-")) {
                return minimumInclusive + (maximumInclusive - minimumInclusive) / 2;
            }
            return delegated;
        }

        @Override
        public <T> T pickWeighted(String sequence, Map<T, Integer> weights) {
            final T delegated = delegate.pickWeighted(sequence, weights);
            if (targetForced || !sequence.startsWith("target-selection:")) {
                return delegated;
            }
            for (final var candidate : weights.keySet()) {
                if (candidate instanceof BattlePersonage personage
                    && personage.id().equals(forcedTargetId)) {
                    targetForced = true;
                    return candidate;
                }
            }
            return delegated;
        }

        @Override
        public <T> List<T> shuffle(String sequence, List<T> values) {
            return delegate.shuffle(sequence, values);
        }
    }
}
