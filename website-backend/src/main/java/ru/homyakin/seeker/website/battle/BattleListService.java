package ru.homyakin.seeker.website.battle;

import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Optional;

@Service
public class BattleListService {
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 100;

    private final BattleListDao battleListDao;

    public BattleListService(BattleListDao battleListDao) {
        this.battleListDao = battleListDao;
    }

    public BattleListResponse listBattles(
        Optional<String> typesParam,
        Optional<String> group,
        Optional<Long> beforeId,
        Optional<Integer> limitParam
    ) {
        final var types = parseTypes(typesParam);
        final var limit = clampLimit(limitParam.orElse(DEFAULT_LIMIT));
        final var items = battleListDao.findBattles(types, group, beforeId, limit);
        return new BattleListResponse(items);
    }

    private EnumSet<BattleType> parseTypes(Optional<String> typesParam) {
        if (typesParam.isEmpty() || typesParam.get().isBlank()) {
            return EnumSet.allOf(BattleType.class);
        }
        final var types = EnumSet.noneOf(BattleType.class);
        for (final var part : typesParam.get().split(",")) {
            BattleType.fromParam(part).ifPresent(types::add);
        }
        if (types.isEmpty()) {
            return EnumSet.allOf(BattleType.class);
        }
        return types;
    }

    private int clampLimit(int limit) {
        if (limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
