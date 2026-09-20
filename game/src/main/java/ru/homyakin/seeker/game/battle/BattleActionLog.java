package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BattleActionLog {
    private final List<BattleEvent> events = new ArrayList<>();
    private final List<BattleTraceEvent> traceEvents = new ArrayList<>();
    private long nextTurnId = 1;
    private long nextAttemptId = 1;

    void add(BattleEvent event) {
        events.add(event);
    }

    void addAll(List<BattleEvent> events) {
        this.events.addAll(events);
    }

    void addTrace(BattleTraceEvent event) {
        traceEvents.add(event);
    }

    long nextTurnId() {
        final long result = nextTurnId;
        nextTurnId = Math.incrementExact(nextTurnId);
        return result;
    }

    long nextAttemptId() {
        final long result = nextAttemptId;
        nextAttemptId = Math.incrementExact(nextAttemptId);
        return result;
    }

    @JsonProperty("events")
    public List<BattleEvent> events() {
        return events;
    }

    @JsonIgnore
    public List<BattleTraceEvent> traceEvents() {
        return Collections.unmodifiableList(traceEvents);
    }
}
