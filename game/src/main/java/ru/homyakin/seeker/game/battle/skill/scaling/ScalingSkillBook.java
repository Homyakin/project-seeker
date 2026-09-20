package ru.homyakin.seeker.game.battle.skill.scaling;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;

/** Runtime state for the explicitly versioned scalable skills of one combatant. */
public final class ScalingSkillBook {
    private static final int MAX_POINTS = 8;

    private final Map<ActiveEnum, Integer> points = new EnumMap<>(ActiveEnum.class);
    private final Map<ActiveEnum, SkillFormulaVersion> versions = new EnumMap<>(ActiveEnum.class);
    private final Map<ActiveEnum, Integer> cooldowns = new EnumMap<>(ActiveEnum.class);
    private final Set<ActiveEnum> highCooldownNext = EnumSet.noneOf(ActiveEnum.class);
    private boolean berserkActivated;
    private int accumulationCharges;
    private boolean tempoBreakImmune;
    private UUID penetrationTargetId;
    private int penetrationFollowUpsRemaining;

    public ScalingSkillBook(Map<ActiveEnum, Integer> skillPoints) {
        this(skillPoints, defaultVersions(skillPoints));
    }

    public ScalingSkillBook(
        Map<ActiveEnum, Integer> skillPoints,
        Map<ActiveEnum, SkillFormulaVersion> skillVersions
    ) {
        for (final var entry : skillPoints.entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalArgumentException("skill must be specified");
            }
            if (entry.getValue() == null || entry.getValue() < 0) {
                throw new IllegalArgumentException("skill points must be non-negative");
            }
            if (entry.getValue() > 0) {
                points.put(entry.getKey(), Math.min(MAX_POINTS, entry.getValue()));
                final var version = skillVersions.get(entry.getKey());
                if (version != SkillFormulaVersion.SCALING_SKILLS_V1
                    && version != SkillFormulaVersion.SCALING_SKILLS_V2) {
                    throw new IllegalArgumentException(
                        "Scaling formula version must be specified for " + entry.getKey()
                    );
                }
                versions.put(entry.getKey(), version);
            }
        }
    }

    private static Map<ActiveEnum, SkillFormulaVersion> defaultVersions(Map<ActiveEnum, Integer> skillPoints) {
        final var result = new EnumMap<ActiveEnum, SkillFormulaVersion>(ActiveEnum.class);
        for (final var skill : skillPoints.keySet()) {
            result.put(skill, SkillFormulaVersion.SCALING_SKILLS_V1);
        }
        return result;
    }

    public boolean has(ActiveEnum skill) {
        return points.containsKey(skill);
    }

    public int points(ActiveEnum skill) {
        return points.getOrDefault(skill, 0);
    }

    public SkillFormulaVersion version(ActiveEnum skill) {
        final var version = versions.get(skill);
        if (version == null) {
            throw new IllegalArgumentException("skill is not present: " + skill);
        }
        return version;
    }

    public boolean ready(ActiveEnum skill) {
        return has(skill) && cooldowns.getOrDefault(skill, 0) == 0;
    }

    public int cooldown(ActiveEnum skill) {
        return cooldowns.getOrDefault(skill, 0);
    }

    public Set<ActiveEnum> coolingAtTurnStart() {
        final var result = EnumSet.noneOf(ActiveEnum.class);
        for (final var entry : cooldowns.entrySet()) {
            if (entry.getValue() > 0) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public void finishTurn(Set<ActiveEnum> coolingAtTurnStart) {
        for (final var skill : coolingAtTurnStart) {
            final int remaining = cooldowns.getOrDefault(skill, 0);
            if (remaining > 0) {
                cooldowns.put(skill, remaining - 1);
            }
        }
    }

    public void startCooldown(ActiveEnum skill, int completedOwnTurns) {
        if (!has(skill)) {
            throw new IllegalArgumentException("skill is not present: " + skill);
        }
        if (completedOwnTurns < 0) {
            throw new IllegalArgumentException("cooldown must be non-negative");
        }
        cooldowns.put(skill, completedOwnTurns);
    }

    /**
     * Returns the lower or upper alternating cooldown. The lower value is selected first;
     * the phase advances only when the skill actually starts its cooldown.
     */
    public int startAlternatingCooldown(ActiveEnum skill, int lower, int upper) {
        if (lower < 0 || upper < lower) {
            throw new IllegalArgumentException("invalid alternating cooldown bounds");
        }
        final boolean useUpper = highCooldownNext.contains(skill);
        final int selected = useUpper ? upper : lower;
        startCooldown(skill, selected);
        if (useUpper) {
            highCooldownNext.remove(skill);
        } else {
            highCooldownNext.add(skill);
        }
        return selected;
    }

    public boolean berserkActivated() {
        return berserkActivated;
    }

    public void activateBerserk() {
        berserkActivated = true;
    }

    public int accumulationCharges() {
        return accumulationCharges;
    }

    public void addAccumulationCharge() {
        accumulationCharges = Math.min(3, accumulationCharges + 1);
    }

    public void clearAccumulationCharges() {
        accumulationCharges = 0;
    }

    public boolean tempoBreakImmune() {
        return tempoBreakImmune;
    }

    public void setTempoBreakImmune(boolean value) {
        tempoBreakImmune = value;
    }

    public Optional<UUID> penetrationTargetId() {
        return Optional.ofNullable(penetrationTargetId);
    }

    public int penetrationFollowUpsRemaining() {
        return penetrationFollowUpsRemaining;
    }

    public void lockPenetrationTarget(UUID targetId, int followUps) {
        if (!has(ActiveEnum.PENETRATION)) {
            throw new IllegalStateException("PENETRATION is not present");
        }
        if (followUps <= 0) {
            throw new IllegalArgumentException("followUps must be positive");
        }
        penetrationTargetId = Objects.requireNonNull(targetId, "targetId");
        penetrationFollowUpsRemaining = followUps;
    }

    public void consumePenetrationFollowUp() {
        if (penetrationTargetId == null || penetrationFollowUpsRemaining <= 0) {
            throw new IllegalStateException("PENETRATION target is not locked");
        }
        penetrationFollowUpsRemaining--;
        if (penetrationFollowUpsRemaining == 0) {
            clearPenetrationTarget();
        }
    }

    public void clearPenetrationTarget() {
        penetrationTargetId = null;
        penetrationFollowUpsRemaining = 0;
    }

    public void clearPenetrationTarget(UUID targetId) {
        if (Objects.equals(penetrationTargetId, targetId)) {
            clearPenetrationTarget();
        }
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }
}
