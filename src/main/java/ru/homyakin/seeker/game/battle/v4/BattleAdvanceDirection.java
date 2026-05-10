package ru.homyakin.seeker.game.battle.v4;

/**
 * Movement along the {@link BattleContext} line array: first team advances toward higher line indices, second toward lower.
 */
public enum BattleAdvanceDirection {
    TOWARD_SECOND_TEAM(1),
    TOWARD_FIRST_TEAM(-1);

    private final int indexDelta;

    BattleAdvanceDirection(int indexDelta) {
        this.indexDelta = indexDelta;
    }

    public int indexDelta() {
        return indexDelta;
    }
}
