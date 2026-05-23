package ru.homyakin.seeker.game.battle.skill;

public enum SkillRank {
    FIRST(1),
    SECOND(2),
    THIRD(4),
    FOURTH(6),
    FIFTH(8);

    private final int requiredPoints;

    SkillRank(int requiredPoints) {
        this.requiredPoints = requiredPoints;
    }

    public int requiredPoints() {
        return requiredPoints;
    }

    public static SkillRank forPoints(int points) {
        SkillRank best = null;
        for (final var rank : values()) {
            if (points >= rank.requiredPoints && (best == null || rank.requiredPoints > best.requiredPoints)) {
                best = rank;
            }
        }
        if (best == null) {
            throw new IllegalArgumentException("No rank found for points: " + points);
        }
        return best;
    }
}
