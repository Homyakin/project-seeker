package ru.homyakin.seeker.game.battle.v4;

import java.util.ArrayList;
import java.util.List;

public final class BattleActionLog {
    private final List<BattleEvent> events = new ArrayList<>();

    void add(BattleEvent event) {
        events.add(event);
    }

    public List<BattleEvent> events() {
        return events;
    }
}
