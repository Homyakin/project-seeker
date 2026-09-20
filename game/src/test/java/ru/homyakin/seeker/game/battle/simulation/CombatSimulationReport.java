package ru.homyakin.seeker.game.battle.simulation;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import ru.homyakin.seeker.game.battle.BattleEvent;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;

public record CombatSimulationReport(
    String raidType,
    String loadout,
    String composition,
    int difficulty,
    int partySize,
    long seed,
    int iterations,
    int maxRounds,
    int wins,
    double winRate,
    ConfidenceInterval winRate95,
    double medianRounds,
    int p95Rounds,
    TeamMetrics evaluatedTeam,
    TeamMetrics opponents,
    List<StartingPosition> startingPositions,
    List<CausalMetric> causalMetrics,
    List<Boolean> iterationWins
) {
    public CombatSimulationReport {
        startingPositions = List.copyOf(startingPositions);
        causalMetrics = List.copyOf(causalMetrics);
        iterationWins = List.copyOf(iterationWins);
        if (!iterationWins.isEmpty() && iterationWins.size() != iterations) {
            throw new IllegalArgumentException("iterationWins must be empty or contain one value per iteration");
        }
    }

    public CombatSimulationReport(
        String raidType,
        String loadout,
        String composition,
        int difficulty,
        int partySize,
        long seed,
        int iterations,
        int maxRounds,
        int wins,
        double winRate,
        ConfidenceInterval winRate95,
        double medianRounds,
        int p95Rounds,
        TeamMetrics evaluatedTeam,
        TeamMetrics opponents,
        List<CausalMetric> causalMetrics
    ) {
        this(
            raidType,
            loadout,
            composition,
            difficulty,
            partySize,
            seed,
            iterations,
            maxRounds,
            wins,
            winRate,
            winRate95,
            medianRounds,
            p95Rounds,
            evaluatedTeam,
            opponents,
            List.of(),
            causalMetrics,
            List.of()
        );
    }

    public CombatSimulationReport(
        String raidType,
        String loadout,
        String composition,
        int difficulty,
        int partySize,
        long seed,
        int iterations,
        int maxRounds,
        int wins,
        double winRate,
        ConfidenceInterval winRate95,
        double medianRounds,
        int p95Rounds,
        TeamMetrics evaluatedTeam,
        TeamMetrics opponents,
        List<StartingPosition> startingPositions,
        List<CausalMetric> causalMetrics
    ) {
        this(
            raidType,
            loadout,
            composition,
            difficulty,
            partySize,
            seed,
            iterations,
            maxRounds,
            wins,
            winRate,
            winRate95,
            medianRounds,
            p95Rounds,
            evaluatedTeam,
            opponents,
            startingPositions,
            causalMetrics,
            List.of()
        );
    }

    public CombatSimulationReport(
        String raidType,
        String loadout,
        String composition,
        int difficulty,
        int partySize,
        long seed,
        int iterations,
        int maxRounds,
        int wins,
        double winRate,
        ConfidenceInterval winRate95,
        double medianRounds,
        int p95Rounds,
        TeamMetrics evaluatedTeam,
        TeamMetrics opponents
    ) {
        this(
            raidType,
            loadout,
            composition,
            difficulty,
            partySize,
            seed,
            iterations,
            maxRounds,
            wins,
            winRate,
            winRate95,
            medianRounds,
            p95Rounds,
            evaluatedTeam,
            opponents,
            List.of(),
            List.of(),
            List.of()
        );
    }

    /**
     * Drops per-iteration outcomes after every statistic that depends on them has been calculated. The compact copy
     * keeps every field used by the human-readable report and by matrix acceptance.
     */
    public CombatSimulationReport withoutRawOutcomes() {
        if (iterationWins.isEmpty()) {
            return this;
        }
        return new CombatSimulationReport(
            raidType,
            loadout,
            composition,
            difficulty,
            partySize,
            seed,
            iterations,
            maxRounds,
            wins,
            winRate,
            winRate95,
            medianRounds,
            p95Rounds,
            evaluatedTeam,
            opponents,
            startingPositions,
            causalMetrics,
            List.of()
        );
    }

    public String markdown() {
        final var summary = String.format(
            Locale.ROOT,
            """
                | Рейд | Сборка | Состав | Сложность | Размер группы | Начальное значение | Бои | Предел раундов | Доля побед (95%% ДИ) | Раунды p50/p95 |
                |---|---|---|---:|---:|---:|---:|---:|---:|---:|
                | %s | %s | %s | %d | %d | %d | %d | %d | %.2f%% [%.2f%%, %.2f%%] | %.1f / %d |

                | Команда | Выжившие | Остаток здоровья | Остаток здоровья, %% | Нанесено урона | Получено урона | Ходы |
                |---|---:|---:|---:|---:|---:|---:|
                | Проверяемая | %.2f | %.2f | %.2f%% | %.2f | %.2f | %.2f |
                | Противники | %.2f | %.2f | %.2f%% | %.2f | %.2f | %.2f |

                | Команда | Обычный урон | Урон умений | Периодический урон | Лечение | Перехваты | Перемещения | Разряды | Окна умений |
                |---|---:|---:|---:|---:|---:|---:|---:|---:|
                | Проверяемая | %.2f | %.2f | %.2f | %.2f | %.2f | %.2f | %.2f | %.2f |
                | Противники | %.2f | %.2f | %.2f | %.2f | %.2f | %.2f | %.2f | %.2f |

                | Команда | Снято очерёдности | Выборы целей | Получено угрозы | Потеряно угрозы |
                |---|---:|---:|---:|---:|
                | Проверяемая | %.2f | %.2f | %.2f | %.2f |
                | Противники | %.2f | %.2f | %.2f | %.2f |

                | Команда | Изменение угрозы: попадание | Изменение угрозы: устранение | Изменение угрозы: полученный урон |
                |---|---:|---:|---:|
                | Проверяемая | %.2f | %.2f | %.2f |
                | Противники | %.2f | %.2f | %.2f |
                """,
            escape(raidType),
            escape(loadout),
            escape(composition),
            difficulty,
            partySize,
            seed,
            iterations,
            maxRounds,
            winRate * 100,
            winRate95.lower() * 100,
            winRate95.upper() * 100,
            medianRounds,
            p95Rounds,
            evaluatedTeam.averageSurvivors(),
            evaluatedTeam.averageRemainingHealth(),
            evaluatedTeam.averageRemainingHealthPercent(),
            evaluatedTeam.averageDamageDealt(),
            evaluatedTeam.averageDamageTaken(),
            evaluatedTeam.averageTurns(),
            opponents.averageSurvivors(),
            opponents.averageRemainingHealth(),
            opponents.averageRemainingHealthPercent(),
            opponents.averageDamageDealt(),
            opponents.averageDamageTaken(),
            opponents.averageTurns(),
            evaluatedTeam.averageNormalDamageDealt(),
            evaluatedTeam.averageSkillDamageDealt(),
            evaluatedTeam.averagePeriodicDamageDealt(),
            evaluatedTeam.averageHealing(),
            evaluatedTeam.averageInterceptions(),
            evaluatedTeam.averageMovements(),
            evaluatedTeam.averageDischarges(),
            evaluatedTeam.averageSkillWindowsUsed(),
            opponents.averageNormalDamageDealt(),
            opponents.averageSkillDamageDealt(),
            opponents.averagePeriodicDamageDealt(),
            opponents.averageHealing(),
            opponents.averageInterceptions(),
            opponents.averageMovements(),
            opponents.averageDischarges(),
            opponents.averageSkillWindowsUsed(),
            evaluatedTeam.averageInitiativeRemoved(),
            evaluatedTeam.averageTargetSelections(),
            evaluatedTeam.averageThreatGained(),
            evaluatedTeam.averageThreatLost(),
            opponents.averageInitiativeRemoved(),
            opponents.averageTargetSelections(),
            opponents.averageThreatGained(),
            opponents.averageThreatLost(),
            evaluatedTeam.averageNormalHitThreatDelta(),
            evaluatedTeam.averageKillThreatDelta(),
            evaluatedTeam.averageDamageTakenThreatDelta(),
            opponents.averageNormalHitThreatDelta(),
            opponents.averageKillThreatDelta(),
            opponents.averageDamageTakenThreatDelta()
        );
        final var positions = startingPositionsMarkdown();
        if (causalMetrics.isEmpty()) {
            return summary + positions;
        }
        final var details = new StringBuilder()
            .append("\n| Показатель | Источник | Умение | Цель | Причина изменения угрозы | Среднее за бой |\n")
            .append("|---|---|---|---|---|---:|\n");
        for (final var metric : causalMetrics) {
            details.append("| ")
                .append(metricName(metric.type()))
                .append(" | ")
                .append(escape(metric.source().displayName()))
                .append(" | ")
                .append(metric.skill().map(CombatSimulationReport::skillName).orElse("—"))
                .append(" | ")
                .append(metric.target().map(ParticipantRef::displayName).map(CombatSimulationReport::escape).orElse("—"))
                .append(" | ")
                .append(metric.threatReason().map(CombatSimulationReport::threatReasonName).orElse("—"))
                .append(" | ")
                .append(String.format(Locale.ROOT, "%.2f", metric.averagePerBattle()))
                .append(" |\n");
        }
        return summary + positions + details;
    }

    private String startingPositionsMarkdown() {
        if (startingPositions.isEmpty()) {
            return "";
        }
        final var result = new StringBuilder()
            .append("\n| Участник | Заявленная линия | Фактическая линия после сближения | ")
            .append("Индекс | До ближайшего врага | Дальность |\n")
            .append("|---|---|---|---:|---:|---:|\n");
        for (final var position : startingPositions) {
            result.append("| ")
                .append(escape(position.participant().displayName()))
                .append(" | ")
                .append(position.requestedLine())
                .append(" | ")
                .append(position.actualLine())
                .append(" | ")
                .append(position.actualLineIndex())
                .append(" | ")
                .append(position.distanceToNearestEnemy())
                .append(" | ")
                .append(position.range())
                .append(" |\n");
        }
        return result.toString();
    }

    private static String metricName(CausalMetricType type) {
        return switch (type) {
            case NORMAL_DAMAGE -> "Обычный урон";
            case SKILL_DAMAGE -> "Прямой урон умения";
            case PERIODIC_DAMAGE -> "Периодический урон";
            case HEALING -> "Лечение";
            case INTERCEPTION -> "Перехват";
            case MOVEMENT -> "Перемещение";
            case DISCHARGE -> "Разряд";
            case SKILL_WINDOW -> "Срабатывание умения";
            case INITIATIVE_REMOVED -> "Снятая очерёдность";
            case TARGET_SELECTION -> "Выбор цели";
            case THREAT_CHANGE -> "Изменение угрозы";
        };
    }

    private static String skillName(ActiveEnum skill) {
        return switch (skill) {
            case COUNTER_ATTACK -> "Контрудар";
            case DOUBLE_ATTACK -> "Двойная атака";
            case BERSERK -> "Берсерк";
            case BLEEDING -> "Кровотечение";
            case SELF_HEAL -> "Самолечение";
            case THORNS -> "Шипы";
            case HIT_AND_RUN -> "Выпад и отход";
            case KNOCKBACK -> "Отброс";
            case PRECISE_STRIKE -> "Точный удар";
            case RETREAT -> "Отступление";
            case FEINT -> "Финт";
            case GUARD -> "Заслон";
            case PENETRATION -> "Проникновение";
            case ACCUMULATION -> "Накопление";
            case TEMPO_BREAK -> "Срыв темпа";
        };
    }

    private static String threatReasonName(BattleEvent.ThreatReason reason) {
        return switch (reason) {
            case DAMAGE_TAKEN -> "Полученный урон";
            case NORMAL_HIT -> "Обычное попадание";
            case KILL -> "Устранение";
        };
    }

    private static String escape(String value) {
        return value.replace("|", "\\|");
    }

    public record ConfidenceInterval(double lower, double upper) {
    }

    public enum TeamSide {
        EVALUATED("Проверяемая"),
        OPPONENTS("Противники");

        private final String displayName;

        TeamSide(String displayName) {
            this.displayName = displayName;
        }
    }

    public record ParticipantRef(TeamSide side, int index, String name) {
        public ParticipantRef {
            if (index < 0) {
                throw new IllegalArgumentException("Participant index must be non-negative");
            }
            if (name == null || name.isBlank()) {
                name = "участник " + (index + 1);
            }
        }

        public String displayName() {
            return side.displayName + " №" + (index + 1) + " · " + name;
        }
    }

    /** Exact battlefield placement captured after BattleContext's free pre-battle approach. */
    public record StartingPosition(
        ParticipantRef participant,
        Position requestedLine,
        Position actualLine,
        int actualLineIndex,
        int distanceToNearestEnemy,
        int range
    ) {
        public StartingPosition {
            if (actualLineIndex < 0 || distanceToNearestEnemy <= 0 || range <= 0) {
                throw new IllegalArgumentException("Invalid starting-position geometry");
            }
        }
    }

    public enum CausalMetricType {
        NORMAL_DAMAGE,
        SKILL_DAMAGE,
        PERIODIC_DAMAGE,
        HEALING,
        INTERCEPTION,
        MOVEMENT,
        DISCHARGE,
        SKILL_WINDOW,
        INITIATIVE_REMOVED,
        TARGET_SELECTION,
        THREAT_CHANGE,
    }

    public record CausalMetric(
        CausalMetricType type,
        ParticipantRef source,
        Optional<ParticipantRef> target,
        Optional<ActiveEnum> skill,
        Optional<BattleEvent.ThreatReason> threatReason,
        double averagePerBattle
    ) {
        public CausalMetric {
            target = target == null ? Optional.empty() : target;
            skill = skill == null ? Optional.empty() : skill;
            threatReason = threatReason == null ? Optional.empty() : threatReason;
        }
    }

    public record TeamMetrics(
        double averageSurvivors,
        double averageRemainingHealth,
        double averageRemainingHealthPercent,
        double averageDamageDealt,
        double averageDamageTaken,
        double averageTurns,
        double averageNormalDamageDealt,
        double averageSkillDamageDealt,
        double averagePeriodicDamageDealt,
        double averageHealing,
        double averageInterceptions,
        double averageMovements,
        double averageDischarges,
        double averageSkillWindowsUsed,
        double averageInitiativeRemoved,
        double averageTargetSelections,
        double averageThreatGained,
        double averageThreatLost,
        double averageNormalHitThreatDelta,
        double averageKillThreatDelta,
        double averageDamageTakenThreatDelta
    ) {
        public TeamMetrics(
            double averageSurvivors,
            double averageRemainingHealth,
            double averageRemainingHealthPercent,
            double averageDamageDealt,
            double averageDamageTaken,
            double averageTurns,
            double averageNormalDamageDealt,
            double averageSkillDamageDealt,
            double averagePeriodicDamageDealt,
            double averageHealing,
            double averageInterceptions,
            double averageMovements,
            double averageDischarges,
            double averageInitiativeRemoved,
            double averageTargetSelections,
            double averageThreatGained,
            double averageThreatLost
        ) {
            this(
                averageSurvivors,
                averageRemainingHealth,
                averageRemainingHealthPercent,
                averageDamageDealt,
                averageDamageTaken,
                averageTurns,
                averageNormalDamageDealt,
                averageSkillDamageDealt,
                averagePeriodicDamageDealt,
                averageHealing,
                averageInterceptions,
                averageMovements,
                averageDischarges,
                0,
                averageInitiativeRemoved,
                averageTargetSelections,
                averageThreatGained,
                averageThreatLost,
                0,
                0,
                0
            );
        }

        public TeamMetrics(
            double averageSurvivors,
            double averageRemainingHealth,
            double averageRemainingHealthPercent,
            double averageDamageDealt,
            double averageDamageTaken,
            double averageTurns
        ) {
            this(
                averageSurvivors,
                averageRemainingHealth,
                averageRemainingHealthPercent,
                averageDamageDealt,
                averageDamageTaken,
                averageTurns,
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
    }
}
