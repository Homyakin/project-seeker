package ru.homyakin.seeker.website.battle;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/launched-event/{launchedEventId}")
public class BattleLogController {
    private final BattleLogService battleLogService;

    public BattleLogController(BattleLogService battleLogService) {
        this.battleLogService = battleLogService;
    }

    @GetMapping("/battle-init")
    public ResponseEntity<JsonNode> getInitState(@PathVariable("launchedEventId") long launchedEventId) {
        return battleLogService.getInitState(launchedEventId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/battle-log")
    public ResponseEntity<JsonNode> getActionLog(@PathVariable("launchedEventId") long launchedEventId) {
        return battleLogService.getActionLog(launchedEventId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
