package ru.homyakin.seeker.game.battle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BattleLine {
    private final boolean firstTeam;
    private final Position position;
    private final Map<UUID, BattlePersonage> battlePersonages;

    public BattleLine(boolean firstTeam, Position position, Map<UUID, BattlePersonage> battlePersonages) {
        this.firstTeam = firstTeam;
        this.position = position;
        this.battlePersonages = new HashMap<>(battlePersonages);
    }

    public boolean firstTeam() {
        return firstTeam;
    }

    public Position position() {
        return position;
    }

    public Map<UUID, BattlePersonage> battlePersonages() {
        return battlePersonages;
    }

    void addPersonage(BattlePersonage personage) {
        battlePersonages.put(personage.id(), personage);
    }

    void removePersonage(UUID id) {
        battlePersonages.remove(id);
    }
}
