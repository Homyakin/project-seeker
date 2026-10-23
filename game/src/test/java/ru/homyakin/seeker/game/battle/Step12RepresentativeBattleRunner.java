package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Build;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.utils.RandomUtils;

/** Runs bounded representative battles while retaining observations from unfinished battles. */
final class Step12RepresentativeBattleRunner {
    private Step12RepresentativeBattleRunner() {
    }

    static Observation observe(
        List<Build> left,
        List<Build> right,
        int level,
        long seed,
        boolean reverse,
        int maxRounds,
        Set<ActiveEnum> disabledLeft,
        Set<ActiveEnum> disabledRight
    ) {
        if (maxRounds <= 0) {
            throw new IllegalArgumentException("maxRounds must be positive");
        }
        final var leftBuilds = List.copyOf(left);
        final var rightBuilds = List.copyOf(right);
        if (leftBuilds.isEmpty() || rightBuilds.isEmpty()) {
            throw new IllegalArgumentException("Both representative teams must be non-empty");
        }
        final var leftDisabled = Set.copyOf(disabledLeft);
        final var rightDisabled = Set.copyOf(disabledRight);
        return RandomUtils.withSeed(seed, () -> {
            final var leftTeam = team(leftBuilds, level, leftDisabled);
            final var rightTeam = team(rightBuilds, level, rightDisabled);
            return run(leftBuilds, rightBuilds, leftTeam, rightTeam, seed, reverse, maxRounds);
        });
    }

    private static List<BattlePersonage> team(List<Build> builds, int level, Set<ActiveEnum> disabled) {
        return builds.stream().map(build -> {
            final var items = build.items(level).stream().map(item ->
                item.modifier().filter(modifier -> disabled.contains(modifier.activeEnum())).isPresent()
                    ? new Item(item.object(), Optional.empty(), item.rarity(), item.enhanceLevel())
                    : item
            ).toList();
            final var personage = BattlePersonage.forScalingSkillsV2Combat(
                items,
                build.position(),
                Map.of(),
                Optional.of(build.code())
            );
            personage.setTargetingTactic(build.tactic());
            return personage;
        }).toList();
    }

    private static Observation run(
        List<Build> leftBuilds,
        List<Build> rightBuilds,
        List<BattlePersonage> leftTeam,
        List<BattlePersonage> rightTeam,
        long seed,
        boolean reverse,
        int maxRounds
    ) {
        final var firstTeam = reverse ? rightTeam : leftTeam;
        final var secondTeam = reverse ? leftTeam : rightTeam;
        final var random = new SeededBattleRandom(seed);
        final var context = new BattleContext(firstTeam, secondTeam, random);
        final var log = new BattleActionLog();
        final var initialPositions = new ArrayList<String>();
        addPositions(initialPositions, "LEFT", leftBuilds, leftTeam);
        addPositions(initialPositions, "RIGHT", rightBuilds, rightTeam);
        final var firstAliveTeam = context.firstAliveTeam();
        final var secondAliveTeam = context.secondAliveTeam();
        int rounds = 0;
        boolean timedOut = false;
        while (!firstAliveTeam.isEmpty() && !secondAliveTeam.isEmpty()) {
            if (rounds >= maxRounds) {
                timedOut = true;
                break;
            }
            ++rounds;
            log.add(new BattleEvent.RoundStarted(rounds));
            final var movers = new ArrayList<Mover>();
            for (final var personage : firstAliveTeam.values()) {
                if (personage.tick(log, rounds)) {
                    movers.add(new Mover(personage, secondAliveTeam));
                }
            }
            for (final var personage : secondAliveTeam.values()) {
                if (personage.tick(log, rounds)) {
                    movers.add(new Mover(personage, firstAliveTeam));
                }
            }
            final var orderedMovers = context.usesScalingSkillOrder()
                ? random.shuffle("turn-order:" + rounds, movers)
                : RandomUtils.shuffle(movers);
            for (final var mover : orderedMovers) {
                if (mover.enemyAliveTeam().isEmpty()) {
                    break;
                }
                if (mover.personage().move(context, log, rounds)) {
                    break;
                }
                context.pruneDefeated();
            }
        }
        final var leftMeasure = measure(leftTeam, log.events(), log.traceEvents());
        final var rightMeasure = measure(rightTeam, log.events(), log.traceEvents());
        final String outcome;
        if (timedOut) {
            outcome = "TIMEOUT";
        } else if (leftMeasure.survivors() > 0) {
            outcome = "WIN";
        } else if (rightMeasure.survivors() > 0) {
            outcome = "LOSS";
        } else {
            outcome = "DRAW";
        }
        return new Observation(
            outcome,
            rounds,
            leftMeasure,
            rightMeasure,
            log.events(),
            log.traceEvents(),
            initialPositions
        );
    }

    private static void addPositions(
        List<String> result,
        String side,
        List<Build> builds,
        List<BattlePersonage> team
    ) {
        for (int index = 0; index < team.size(); index++) {
            final var personage = team.get(index);
            result.add("%s:%d:%s:%s:%d".formatted(
                side,
                index,
                builds.get(index).code(),
                personage.startPosition(),
                personage.currentPosition()
            ));
        }
    }

    /** Initiative removed is credited to its source team; received damage is actual health lost. */
    static TeamMeasure measure(
        List<BattlePersonage> team,
        List<BattleEvent> events,
        List<BattleTraceEvent> traces
    ) {
        final var ids = new HashSet<UUID>();
        team.forEach(personage -> ids.add(personage.id()));
        final var attemptedTurns = new HashSet<Long>();
        final var attemptedRounds = new HashSet<PersonageRound>();
        long attempts = 0;
        for (final var trace : traces) {
            if (trace instanceof BattleTraceEvent.NormalAttackAttempt attempt && ids.contains(attempt.attackerId())) {
                attemptedTurns.add(attempt.turnId());
                attemptedRounds.add(new PersonageRound(attempt.attackerId(), attempt.round()));
                attempts++;
            }
        }
        final var movementRounds = new HashSet<PersonageRound>();
        for (final var event : events) {
            if (event instanceof BattleEvent.MovedTowardEnemy movement && ids.contains(movement.personageId())) {
                movementRounds.add(new PersonageRound(movement.personageId(), movement.round()));
            }
        }
        long actions = 0;
        long movementTurns = 0;
        for (final var trace : traces) {
            if (trace instanceof BattleTraceEvent.TurnFinished turn
                && ids.contains(turn.personageId()) && turn.actionPerformed()) {
                actions++;
                if (!attemptedTurns.contains(turn.turnId())
                    && movementRounds.contains(new PersonageRound(turn.personageId(), turn.round()))) {
                    movementTurns++;
                }
            }
        }
        long healing = 0;
        long initiativeRemoved = 0;
        long aliveTicks = 0;
        int longestNoAttemptRounds = 0;
        final var currentNoAttemptRounds = new HashMap<UUID, Integer>();
        for (final var event : events) {
            if (event instanceof BattleEvent.PersonageHealed healed && ids.contains(healed.personageId())) {
                healing += healed.amount();
            } else if (event instanceof BattleEvent.InitiativeDelayed delayed && ids.contains(delayed.sourceId())) {
                initiativeRemoved += delayed.amount();
            } else if (event instanceof BattleEvent.InitiativeAfterTick tick && ids.contains(tick.personageId())) {
                aliveTicks++;
                final int gap = attemptedRounds.contains(new PersonageRound(tick.personageId(), tick.round()))
                    ? 0
                    : currentNoAttemptRounds.getOrDefault(tick.personageId(), 0) + 1;
                currentNoAttemptRounds.put(tick.personageId(), gap);
                longestNoAttemptRounds = Math.max(longestNoAttemptRounds, gap);
            }
        }
        return new TeamMeasure(
            (int) team.stream().filter(BattlePersonage::isAlive).count(),
            team.stream().mapToLong(BattlePersonage::health).sum(),
            actions,
            attempts,
            movementTurns,
            team.stream().mapToLong(BattlePersonage::actualDamageTaken).sum(),
            healing,
            initiativeRemoved,
            aliveTicks,
            longestNoAttemptRounds
        );
    }

    record Observation(
        String outcome,
        int rounds,
        TeamMeasure left,
        TeamMeasure right,
        List<BattleEvent> events,
        List<BattleTraceEvent> traces,
        List<String> initialPositions
    ) {
        Observation {
            events = List.copyOf(events);
            traces = List.copyOf(traces);
            initialPositions = List.copyOf(initialPositions);
        }
    }

    record TeamMeasure(
        int survivors,
        long remainingHealth,
        long actions,
        long attempts,
        long movementTurns,
        long damageTaken,
        long healing,
        long initiativeRemoved,
        long aliveTicks,
        int longestNoAttemptRounds
    ) {
    }

    private record Mover(BattlePersonage personage, Map<UUID, BattlePersonage> enemyAliveTeam) {
    }

    private record PersonageRound(UUID personageId, int round) {
    }
}
