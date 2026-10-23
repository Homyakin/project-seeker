package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.MixedBuild;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.RoleBuild;

/** Заранее записанная матрица слоя смешанных сборок шага 12. */
final class Step12MixedBuildMatrix {
    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static final Pattern ASCII_CODE = Pattern.compile("[A-Z0-9_]+");

    private static final List<Comparison> COMPARISONS = List.of(
        comparison(
            "RAM_GUARD",
            MixedBuild.RAM,
            Optional.of(RoleBuild.GUARDIAN),
            Benchmark.GUARD,
            Optional.of(ActiveEnum.GUARD),
            Optional.of(Metric.GUARD_INTERCEPTIONS_PER_ENEMY_ATTEMPT),
            Optional.empty(),
            Optional.empty(),
            Rule.MECHANICS_PRESENT,
            "«Заслон» Тарана работает; меньшая защита закрепляется точными числами"
        ),
        comparison(
            "RAM_PRESSURE",
            MixedBuild.RAM,
            Optional.of(RoleBuild.BRUISER),
            Benchmark.PRESSURE,
            Optional.of(ActiveEnum.BERSERK),
            Optional.of(Metric.NORMAL_DAMAGE_PER_TURN),
            Optional.of(Metric.NORMAL_DAMAGE_PER_TURN),
            Optional.of(Metric.NORMAL_DAMAGE_PER_TURN),
            Rule.LOWER_THAN_REFERENCE,
            "обычный урон Тарана за ход ниже показателя Громилы"
        ),
        comparison(
            "KEEPER_GUARD",
            MixedBuild.KEEPER,
            Optional.of(RoleBuild.GUARDIAN),
            Benchmark.GUARD,
            Optional.of(ActiveEnum.GUARD),
            Optional.of(Metric.GUARD_INTERCEPTIONS_PER_ENEMY_ATTEMPT),
            Optional.empty(),
            Optional.empty(),
            Rule.MECHANICS_PRESENT,
            "«Заслон» Хранителя работает; меньшие здоровье и угроза проверяются точно"
        ),
        comparison(
            "KEEPER_TEMPO",
            MixedBuild.KEEPER,
            Optional.of(RoleBuild.TACTICIAN_PHYSICAL),
            Benchmark.TEMPO,
            Optional.of(ActiveEnum.TEMPO_BREAK),
            Optional.of(Metric.INITIATIVE_REMOVED_PER_TURN),
            Optional.of(Metric.NORMAL_DAMAGE_PER_TURN),
            Optional.of(Metric.NORMAL_DAMAGE_PER_TURN),
            Rule.LOWER_THAN_REFERENCE,
            "помехи Хранителя работают, а личный урон ниже показателя Тактика"
        ),
        comparison(
            "DUELIST_PRESSURE",
            MixedBuild.DUELIST,
            Optional.of(RoleBuild.BRUISER),
            Benchmark.PRESSURE,
            Optional.of(ActiveEnum.BERSERK),
            Optional.of(Metric.NORMAL_DAMAGE_PER_TURN),
            Optional.of(Metric.NORMAL_DAMAGE_PER_TURN),
            Optional.of(Metric.NORMAL_DAMAGE_PER_TURN),
            Rule.LOWER_THAN_REFERENCE,
            "обычный урон Дуэлянта за ход ниже показателя Громилы"
        ),
        comparison(
            "DUELIST_MOBILITY",
            MixedBuild.DUELIST,
            Optional.of(RoleBuild.SKIRMISHER),
            Benchmark.MOBILITY,
            Optional.of(ActiveEnum.HIT_AND_RUN),
            Optional.of(Metric.HIT_AND_RUN_MOVES_PER_TURN),
            Optional.empty(),
            Optional.empty(),
            Rule.MECHANICS_PRESENT,
            "у Дуэлянта есть срабатывания «Выпада и отхода» на ход"
        ),
        comparison(
            "RAIDER_MOBILITY",
            MixedBuild.RAIDER,
            Optional.of(RoleBuild.SKIRMISHER),
            Benchmark.MOBILITY,
            Optional.of(ActiveEnum.HIT_AND_RUN),
            Optional.of(Metric.HIT_AND_RUN_MOVES_PER_TURN),
            Optional.empty(),
            Optional.empty(),
            Rule.MECHANICS_PRESENT,
            "у Налётчика есть срабатывания «Выпада и отхода» на ход"
        ),
        comparison(
            "RAIDER_ACCESS",
            MixedBuild.RAIDER,
            Optional.of(RoleBuild.ASSASSIN),
            Benchmark.ACCESS,
            Optional.of(ActiveEnum.PENETRATION),
            Optional.of(Metric.PENETRATION_DAMAGE_PER_TURN),
            Optional.of(Metric.PENETRATION_DAMAGE_PER_TURN),
            Optional.of(Metric.PENETRATION_DAMAGE_PER_TURN),
            Rule.LOWER_THAN_REFERENCE,
            "урон «Проникновения» Налётчика за ход ниже показателя Убийцы"
        ),
        comparison(
            "FORMATION_BREAKER_PRESSURE",
            MixedBuild.FORMATION_BREAKER,
            Optional.of(RoleBuild.BRUISER),
            Benchmark.PRESSURE,
            Optional.of(ActiveEnum.BERSERK),
            Optional.of(Metric.NORMAL_DAMAGE_PER_TURN),
            Optional.of(Metric.NORMAL_DAMAGE_PER_TURN),
            Optional.of(Metric.NORMAL_DAMAGE_PER_TURN),
            Rule.LOWER_THAN_REFERENCE,
            "обычный урон Ломателя строя за ход ниже показателя Громилы"
        ),
        comparison(
            "FORMATION_BREAKER_TEMPO",
            MixedBuild.FORMATION_BREAKER,
            Optional.of(RoleBuild.TACTICIAN_PHYSICAL),
            Benchmark.TEMPO,
            Optional.of(ActiveEnum.TEMPO_BREAK),
            Optional.of(Metric.INITIATIVE_REMOVED_PER_TURN),
            Optional.of(Metric.INITIATIVE_REMOVED_PER_TURN),
            Optional.of(Metric.INITIATIVE_REMOVED_PER_TURN),
            Rule.LOWER_THAN_REFERENCE,
            "снятая Ломателем строя очерёдность за ход ниже показателя Тактика"
        ),
        comparison(
            "ARTILLERIST_RANGED",
            MixedBuild.ARTILLERIST,
            Optional.of(RoleBuild.RANGER_PHYSICAL),
            Benchmark.RANGED,
            Optional.empty(),
            Optional.empty(),
            Optional.of(Metric.RANGED_SUSTAINED_DAMAGE_PER_TURN),
            Optional.of(Metric.RANGED_SUSTAINED_DAMAGE_PER_TURN),
            Rule.LOWER_THAN_REFERENCE,
            "устойчивый дальний урон Артиллериста за ход ниже показателя Дальнобойца"
        ),
        comparison(
            "ARTILLERIST_BURST",
            MixedBuild.ARTILLERIST,
            Optional.of(RoleBuild.BREAKER_PHYSICAL),
            Benchmark.BURST,
            Optional.of(ActiveEnum.ACCUMULATION),
            Optional.of(Metric.ACCUMULATION_DAMAGE_PER_TURN),
            Optional.empty(),
            Optional.empty(),
            Rule.MECHANICS_PRESENT,
            "у Артиллериста есть урон «Накопления» на ход"
        ),
        comparison(
            "BLOCKER_RANGED",
            MixedBuild.BLOCKER,
            Optional.of(RoleBuild.RANGER_PHYSICAL),
            Benchmark.RANGED,
            Optional.empty(),
            Optional.empty(),
            Optional.of(Metric.RANGED_SUSTAINED_DAMAGE_PER_TURN),
            Optional.of(Metric.RANGED_SUSTAINED_DAMAGE_PER_TURN),
            Rule.LOWER_THAN_REFERENCE,
            "устойчивый дальний урон Заградителя за ход ниже показателя Дальнобойца"
        ),
        comparison(
            "BLOCKER_CONTROL",
            MixedBuild.BLOCKER,
            Optional.of(RoleBuild.TACTICIAN_PHYSICAL),
            Benchmark.CONTROL,
            Optional.of(ActiveEnum.KNOCKBACK),
            Optional.of(Metric.KNOCKBACK_MOVES_PER_TURN),
            Optional.empty(),
            Optional.of(Metric.INITIATIVE_REMOVED_PER_TURN),
            Rule.CONTROL_DIAGNOSTIC,
            "«Отброс» Заградителя создаёт перемещения и лишает ближних противников попыток; "
                + "показатель Тактика выводится отдельно"
        )
    );

    private Step12MixedBuildMatrix() {
    }

    static List<Comparison> comparisons() {
        return COMPARISONS;
    }

    static List<Cell> cells(long rootSeed) {
        final var cells = new ArrayList<Cell>(
            Math.multiplyExact(COMPARISONS.size(), Step12SimulationFixtures.CONTROL_LEVELS.size())
        );
        for (final var comparison : COMPARISONS) {
            for (final int level : Step12SimulationFixtures.CONTROL_LEVELS) {
                final var code = cellCode(comparison, level);
                cells.add(new Cell(comparison, level, cellSeed(rootSeed, code)));
            }
        }
        return List.copyOf(cells);
    }

    static String cellCode(Comparison comparison, int level) {
        Objects.requireNonNull(comparison, "comparison");
        if (!Step12SimulationFixtures.CONTROL_LEVELS.contains(level)) {
            throw new IllegalArgumentException("Unsupported step 12 mixed-build level: " + level);
        }
        return comparison.code() + "_L" + level;
    }

    static long cellSeed(long rootSeed, String cellCode) {
        Objects.requireNonNull(cellCode, "cellCode");
        var hash = FNV_OFFSET_BASIS;
        for (int index = 0; index < cellCode.length(); index++) {
            hash ^= cellCode.charAt(index);
            hash *= FNV_PRIME;
        }
        var mixed = rootSeed ^ hash;
        mixed = (mixed ^ mixed >>> 30) * 0xbf58476d1ce4e5b9L;
        mixed = (mixed ^ mixed >>> 27) * 0x94d049bb133111ebL;
        return mixed ^ mixed >>> 31;
    }

    private static Comparison comparison(
        String code,
        MixedBuild mixedBuild,
        Optional<RoleBuild> referenceBuild,
        Benchmark benchmark,
        Optional<ActiveEnum> activitySkill,
        Optional<Metric> activityMetric,
        Optional<Metric> priceMetric,
        Optional<Metric> referenceMetric,
        Rule rule,
        String expectation
    ) {
        return new Comparison(
            code,
            mixedBuild,
            referenceBuild,
            benchmark,
            activitySkill,
            activityMetric,
            priceMetric,
            referenceMetric,
            rule,
            expectation
        );
    }

    enum Benchmark {
        GUARD,
        PRESSURE,
        MOBILITY,
        ACCESS,
        RANGED,
        BURST,
        TEMPO,
        CONTROL,
    }

    enum Metric {
        GUARD_INTERCEPTIONS_PER_ENEMY_ATTEMPT,
        NORMAL_DAMAGE_PER_TURN,
        HIT_AND_RUN_MOVES_PER_TURN,
        PENETRATION_DAMAGE_PER_TURN,
        RANGED_SUSTAINED_DAMAGE_PER_TURN,
        ACCUMULATION_DAMAGE_PER_TURN,
        INITIATIVE_REMOVED_PER_TURN,
        KNOCKBACK_MOVES_PER_TURN,
    }

    enum Rule {
        LOWER_THAN_REFERENCE,
        MECHANICS_PRESENT,
        CONTROL_DIAGNOSTIC,
    }

    record Comparison(
        String code,
        MixedBuild mixedBuild,
        Optional<RoleBuild> referenceBuild,
        Benchmark benchmark,
        Optional<ActiveEnum> activitySkill,
        Optional<Metric> activityMetric,
        Optional<Metric> priceMetric,
        Optional<Metric> referenceMetric,
        Rule rule,
        String expectation
    ) {
        Comparison {
            if (code == null || !ASCII_CODE.matcher(code).matches()) {
                throw new IllegalArgumentException("Comparison code must be non-blank ASCII: " + code);
            }
            Objects.requireNonNull(mixedBuild, "mixedBuild");
            referenceBuild = Objects.requireNonNull(referenceBuild, "referenceBuild");
            Objects.requireNonNull(benchmark, "benchmark");
            activitySkill = Objects.requireNonNull(activitySkill, "activitySkill");
            activityMetric = Objects.requireNonNull(activityMetric, "activityMetric");
            priceMetric = Objects.requireNonNull(priceMetric, "priceMetric");
            referenceMetric = Objects.requireNonNull(referenceMetric, "referenceMetric");
            Objects.requireNonNull(rule, "rule");
            if (expectation == null || expectation.isBlank()) {
                throw new IllegalArgumentException("Comparison expectation must not be blank");
            }
            if (activitySkill.isPresent() != activityMetric.isPresent()) {
                throw new IllegalArgumentException("Activity skill and metric must be present together");
            }
            if (rule == Rule.LOWER_THAN_REFERENCE
                && (referenceBuild.isEmpty() || priceMetric.isEmpty() || referenceMetric.isEmpty())) {
                throw new IllegalArgumentException("A price comparison requires a reference and both metrics");
            }
            if (rule == Rule.MECHANICS_PRESENT && activityMetric.isEmpty()) {
                throw new IllegalArgumentException("A mechanics comparison requires an activity metric");
            }
            if (rule == Rule.CONTROL_DIAGNOSTIC
                && (referenceBuild.orElse(null) != RoleBuild.TACTICIAN_PHYSICAL
                    || referenceMetric.orElse(null) != Metric.INITIATIVE_REMOVED_PER_TURN
                    || activitySkill.orElse(null) != ActiveEnum.KNOCKBACK)) {
                throw new IllegalArgumentException("Control diagnosis is reserved for standalone knockback");
            }
        }
    }

    record Cell(Comparison comparison, int level, long seed) {
        Cell {
            Objects.requireNonNull(comparison, "comparison");
            if (!Step12SimulationFixtures.CONTROL_LEVELS.contains(level)) {
                throw new IllegalArgumentException("Unsupported step 12 mixed-build level: " + level);
            }
        }

        String code() {
            return cellCode(comparison, level);
        }
    }
}
