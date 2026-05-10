package ru.homyakin.seeker.game.battle.v4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BattleContext {
    /**
     * Ordered from first team's own back toward second team's own back (e.g. {@code [BACK_1, FRONT_1, FRONT_2, BACK_2]} if mids are empty).
     * Callers ensure both teams expose at least one non-empty line toward the other so {@code 0 < firstTeamLineCount < lines.size()}.
     */
    private final List<BattleLine> lines;
    private final Map<UUID, BattlePersonage> firstAliveTeam;
    private final Map<UUID, BattlePersonage> secondAliveTeam;

    public BattleContext(List<BattlePersonage> firstTeam, List<BattlePersonage> secondTeam) {
        this.firstAliveTeam = new HashMap<>();
        for (final var p : firstTeam) {
            if (p.isAlive()) {
                firstAliveTeam.put(p.id(), p);
            }
        }
        this.secondAliveTeam = new HashMap<>();
        for (final var p : secondTeam) {
            if (p.isAlive()) {
                secondAliveTeam.put(p.id(), p);
            }
        }

        final var firstLines = linesFromTeam(firstTeam, true);
        final var secondLines = linesFromTeam(secondTeam, false);
        final var combined = new ArrayList<BattleLine>();
        combined.addAll(firstLines);
        combined.addAll(secondLines);
        this.lines = List.copyOf(combined);

        int index = 0;
        for (final var line : firstLines) {
            for (final var personage : line.battlePersonages().values()) {
                personage.placeOnBattlefield(index, BattleAdvanceDirection.TOWARD_SECOND_TEAM);
            }
            index++;
        }
        for (final var line : secondLines) {
            for (final var personage : line.battlePersonages().values()) {
                personage.placeOnBattlefield(index, BattleAdvanceDirection.TOWARD_FIRST_TEAM);
            }
            index++;
        }

        final int firstTeamLineCount = firstLines.size();
        final int totalLines = lines.size();
        adjustLineIndexForRange(firstTeam, firstTeamLineCount, totalLines, true);
        adjustLineIndexForRange(secondTeam, firstTeamLineCount, totalLines, false);
    }

    public Map<UUID, BattlePersonage> firstAliveTeam() {
        return firstAliveTeam;
    }

    public Map<UUID, BattlePersonage> secondAliveTeam() {
        return secondAliveTeam;
    }

    public Map<UUID, BattlePersonage> enemyAliveTeam(BattlePersonage personage) {
        if (personage.advanceDirection() == BattleAdvanceDirection.TOWARD_SECOND_TEAM) {
            return secondAliveTeam;
        } else {
            return firstAliveTeam;
        }
    }

    /**
     * Nearest enemy is across the gap at the first index of the other team; distance is in whole-line steps.
     */
    private void adjustLineIndexForRange(
        List<BattlePersonage> team,
        int firstTeamLineCount,
        int totalLines,
        boolean firstTeam
    ) {
        for (final var personage : team) {
            final int from = personage.currentPosition();
            if (from < 0) {
                continue;
            }
            final int to = firstTeam
                ? lineIndexForFirstTeamInRange(from, personage.range(), firstTeamLineCount)
                : lineIndexForSecondTeamInRange(from, personage.range(), firstTeamLineCount, totalLines);
            if (to != from) {
                relocatePersonage(personage, from, to);
            }
        }
    }

    private static int lineIndexForFirstTeamInRange(int pos, int range, int firstTeamLineCount) {
        final int nearestEnemyLine = firstTeamLineCount;
        final int distanceToEnemy = nearestEnemyLine - pos;
        if (distanceToEnemy <= range) {
            return pos;
        }
        final int raw = nearestEnemyLine - range;
        return Math.min(firstTeamLineCount - 1, Math.max(0, raw));
    }

    private static int lineIndexForSecondTeamInRange(int pos, int range, int firstTeamLineCount, int totalLines) {
        final int nearestEnemyLine = firstTeamLineCount - 1;
        final int distanceToEnemy = pos - nearestEnemyLine;
        if (distanceToEnemy <= range) {
            return pos;
        }
        final int raw = nearestEnemyLine + range;
        return Math.max(firstTeamLineCount, Math.min(totalLines - 1, raw));
    }

    private void relocatePersonage(BattlePersonage personage, int fromIndex, int toIndex) {
        lines.get(fromIndex).removePersonage(personage.id());
        lines.get(toIndex).addPersonage(personage);
        personage.placeOnBattlefield(toIndex, personage.advanceDirection());
    }

    public void moveBackward(BattlePersonage personage, int steps) {
        if (!personage.isAlive() || personage.currentPosition() < 0 || steps <= 0) {
            return;
        }
        final int from = personage.currentPosition();
        final int backwardDelta = -personage.advanceDirection().indexDelta();
        int to = from + backwardDelta * steps;
        final int firstCount = firstTeamLineCount();
        if (personage.advanceDirection() == BattleAdvanceDirection.TOWARD_SECOND_TEAM) {
            to = Math.max(0, Math.min(firstCount - 1, to));
        } else {
            to = Math.max(firstCount, Math.min(lines.size() - 1, to));
        }
        if (to != from) {
            relocatePersonage(personage, from, to);
        }
    }

    private int firstTeamLineCount() {
        int count = 0;
        for (final var line : lines) {
            if (line.firstTeam()) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    public List<BattleLine> lines() {
        return lines;
    }

    private static List<BattleLine> linesFromTeam(List<BattlePersonage> team, boolean firstTeam) {
        final var front = new HashMap<UUID, BattlePersonage>();
        final var mid = new HashMap<UUID, BattlePersonage>();
        final var back = new HashMap<UUID, BattlePersonage>();
        for (final var personage : team) {
            switch (personage.startPosition()) {
                case FRONT -> front.put(personage.id(), personage);
                case MID -> mid.put(personage.id(), personage);
                case BACK -> back.put(personage.id(), personage);
            }
        }
        final boolean hasFront = !front.isEmpty();
        final boolean hasMid = !mid.isEmpty();

        final var result = new ArrayList<BattleLine>();
        if (firstTeam) {
            result.add(new BattleLine(firstTeam, Position.BACK, back));
            if (hasMid || hasFront) {
                result.add(new BattleLine(firstTeam, Position.MID, mid));
            }
            if (hasFront) {
                result.add(new BattleLine(firstTeam, Position.FRONT, front));
            }
        } else {
            if (hasFront) {
                result.add(new BattleLine(firstTeam, Position.FRONT, front));
            }
            if (hasMid || hasFront) {
                result.add(new BattleLine(firstTeam, Position.MID, mid));
            }
            result.add(new BattleLine(firstTeam, Position.BACK, back));
        }
        return result;
    }
}
