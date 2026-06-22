package ru.homyakin.seeker.website.battle;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BattleLogService {
    private final BattleLogDao battleLogDao;

    public BattleLogService(BattleLogDao battleLogDao) {
        this.battleLogDao = battleLogDao;
    }

    public Optional<JsonNode> getInitState(long launchedEventId) {
        return battleLogDao.getInitState(launchedEventId);
    }

    public Optional<JsonNode> getActionLog(long launchedEventId) {
        return battleLogDao.getActionLog(launchedEventId);
    }
}
