package ru.homyakin.seeker.game.battle.v4.skill;

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

    public static SkillRank forPoints(int points) {
        for (final var rank : values()) {
            if (points >= rank.requiredPoints) {
                return rank;
            }
        }
        throw new IllegalArgumentException("No rank found for points: " + points);
    }
}
