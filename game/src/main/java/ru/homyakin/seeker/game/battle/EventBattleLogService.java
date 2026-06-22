package ru.homyakin.seeker.game.battle;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.battle.infra.database.EventBattleLogDao;

@Service
public class EventBattleLogService {
    private final EventBattleLogDao eventBattleLogDao;

    public EventBattleLogService(EventBattleLogDao eventBattleLogDao) {
        this.eventBattleLogDao = eventBattleLogDao;
    }

    public void save(long launchedEventId, BattleResult result) {
        eventBattleLogDao.save(launchedEventId, result.initState(), result.actionLog());
    }
}
