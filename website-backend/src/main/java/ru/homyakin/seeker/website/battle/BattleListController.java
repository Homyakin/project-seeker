package ru.homyakin.seeker.website.battle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/battles")
public class BattleListController {
    private static final Logger logger = LoggerFactory.getLogger(BattleListController.class);

    private final BattleListService battleListService;

    public BattleListController(BattleListService battleListService) {
        this.battleListService = battleListService;
    }

    @GetMapping
    public BattleListResponse listBattles(
        @RequestParam(value = "types", required = false) String types,
        @RequestParam(value = "group", required = false) String group,
        @RequestParam(value = "beforeId", required = false) Long beforeId,
        @RequestParam(value = "limit", required = false) Integer limit
    ) {
        logger.info(
            "Requesting battles list types={} group={} beforeId={} limit={}",
            types, group, beforeId, limit
        );
        return battleListService.listBattles(
            Optional.ofNullable(types),
            Optional.ofNullable(group),
            Optional.ofNullable(beforeId),
            Optional.ofNullable(limit)
        );
    }
}
