package ru.homyakin.seeker.game.battle.effect;

public final class TemporaryMaxRangeBonus {
    private final int delta;
    private int remainingOwnMoves;

    public TemporaryMaxRangeBonus(int delta, int remainingOwnMoves) {
        if (delta <= 0) {
            throw new IllegalArgumentException("delta must be positive");
        }
        if (remainingOwnMoves <= 0) {
            throw new IllegalArgumentException("remainingOwnMoves must be positive");
        }
        this.delta = delta;
        this.remainingOwnMoves = remainingOwnMoves;
    }

    public int delta() {
        return delta;
    }

    /**
     * @return true when this bonus should be removed from the personage
     */
    public boolean tickAtOwnTurnBegin() {
        return --remainingOwnMoves <= 0;
    }
}
