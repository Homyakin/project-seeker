package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBattleRunner.Observation;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Build;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.utils.RandomUtils;

/** Version-aware observations for step 12.3; the historical step 12.2 runner is unchanged. */
final class Step123BattleRunner {
    private static final Map<EquipmentCatalogVersion, Catalog> CATALOGS = new ConcurrentHashMap<>();

    private Step123BattleRunner() {
    }

    static Observation observe(
        List<Gear> left,
        List<Gear> right,
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
        final var leftGear = List.copyOf(left);
        final var rightGear = List.copyOf(right);
        if (leftGear.isEmpty() || rightGear.isEmpty()) {
            throw new IllegalArgumentException("Both representative teams must be non-empty");
        }
        final var leftDisabled = Set.copyOf(disabledLeft);
        final var rightDisabled = Set.copyOf(disabledRight);
        return RandomUtils.withSeed(seed, () -> {
            final var leftTeam = team(leftGear, level, leftDisabled);
            final var rightTeam = team(rightGear, level, rightDisabled);
            return run(leftGear, rightGear, leftTeam, rightTeam, seed, reverse, maxRounds);
        });
    }

    private static List<BattlePersonage> team(List<Gear> gear, int level, Set<ActiveEnum> disabled) {
        return gear.stream().map(equipment -> {
            final var build = equipment.build();
            final var personage = BattlePersonage.forScalingSkillsV2Combat(
                equipment.items(level, disabled),
                build.position(),
                Map.of(),
                Optional.of(build.code())
            );
            personage.setTargetingTactic(build.tactic());
            return personage;
        }).toList();
    }

    private static Observation run(
        List<Gear> leftGear,
        List<Gear> rightGear,
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
        addPositions(initialPositions, "LEFT", leftGear, leftTeam);
        addPositions(initialPositions, "RIGHT", rightGear, rightTeam);
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
        final var leftMeasure = Step12RepresentativeBattleRunner.measure(leftTeam, log.events(), log.traceEvents());
        final var rightMeasure = Step12RepresentativeBattleRunner.measure(rightTeam, log.events(), log.traceEvents());
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
            outcome, rounds, leftMeasure, rightMeasure, log.events(), log.traceEvents(), initialPositions
        );
    }

    private static void addPositions(
        List<String> result,
        String side,
        List<Gear> gear,
        List<BattlePersonage> team
    ) {
        for (int index = 0; index < team.size(); index++) {
            final var personage = team.get(index);
            result.add("%s:%d:%s:%s:%d".formatted(
                side, index, gear.get(index).build().code(), personage.startPosition(), personage.currentPosition()
            ));
        }
    }

    private static Catalog catalog(EquipmentCatalogVersion version) {
        return CATALOGS.computeIfAbsent(version, key -> {
            final var loaded = EquipmentCatalogLoader.loadValidated(key);
            return new Catalog(
                loaded.itemObjects().stream().collect(Collectors.toUnmodifiableMap(
                    ItemObject::code, Function.identity()
                )),
                loaded.modifiers().stream().collect(Collectors.toUnmodifiableMap(
                    Modifier::activeEnum, Function.identity()
                ))
            );
        });
    }

    record Gear(Build build, EquipmentCatalogVersion catalog) {
        Gear {
            Objects.requireNonNull(build, "Build must be specified");
            Objects.requireNonNull(catalog, "Catalog version must be specified");
        }

        List<Item> items(int level) {
            final var selected = Step123BattleRunner.catalog(catalog);
            return build.items(level).stream().map(item -> {
                final var object = selected.objects().get(item.object().code());
                if (object == null) {
                    throw new IllegalArgumentException(catalog + " has no item object: " + item.object().code());
                }
                final var modifier = item.modifier().map(source -> {
                    final var remapped = selected.modifiers().get(source.activeEnum());
                    if (remapped == null) {
                        throw new IllegalArgumentException(catalog + " has no modifier for: " + source.activeEnum());
                    }
                    return remapped;
                });
                return new Item(object, modifier, item.rarity(), item.enhanceLevel());
            }).toList();
        }

        List<Item> items(int level, Set<ActiveEnum> disabled) {
            final var disabledSkills = Set.copyOf(disabled);
            return items(level).stream().map(item ->
                item.modifier().filter(modifier -> disabledSkills.contains(modifier.activeEnum())).isPresent()
                    ? new Item(item.object(), Optional.empty(), item.rarity(), item.enhanceLevel())
                    : item
            ).toList();
        }
    }

    private record Catalog(Map<String, ItemObject> objects, Map<ActiveEnum, Modifier> modifiers) {
    }

    private record Mover(BattlePersonage personage, Map<UUID, BattlePersonage> enemyAliveTeam) {
    }
}
