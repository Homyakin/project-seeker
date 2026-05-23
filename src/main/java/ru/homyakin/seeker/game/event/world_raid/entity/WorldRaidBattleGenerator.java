package ru.homyakin.seeker.game.event.world_raid.entity;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.v4.BattlePersonage;
import ru.homyakin.seeker.game.battle.v4.BattlePersonageStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class WorldRaidBattleGenerator {
    public List<BattlePersonage> generate(ActiveWorldRaid raid) {
        return generate(raid.info());
    }

    public List<BattlePersonage> generate(WorldRaidLaunchedBattleInfo info) {
        return info.personagesOrEmpty().stream()
            .map(personage -> new BattlePersonage(personage, personage.position()))
            .toList();
    }

    public WorldRaidLaunchedBattleInfo launchedFromTemplate(WorldRaidTemplateBattleInfo templateInfo) {
        if (!templateInfo.hasPersonageTemplates()) {
            throw new IllegalStateException("World raid templates must be present in template info");
        }
        return new WorldRaidLaunchedBattleInfo(materializePersonages(templateInfo));
    }

    public WorldRaidLaunchedBattleInfo remainedInfo(
        WorldRaidLaunchedBattleInfo baseInfo,
        List<BattlePersonage> enemies,
        Map<UUID, BattlePersonageStats> statsById
    ) {
        if (!baseInfo.hasPersonages()) {
            throw new IllegalStateException("Launched world raid personages must be present");
        }

        final var sourcePersonages = baseInfo.personagesOrEmpty();
        final var remainedPersonages = new ArrayList<WorldRaidPersonage>();
        for (int i = 0; i < sourcePersonages.size() && i < enemies.size(); i++) {
            final var source = sourcePersonages.get(i);
            final var stats = statsById.get(enemies.get(i).id());
            if (stats.remainHealth() > 0) {
                remainedPersonages.add(source.withHealth(stats.remainHealth()));
            }
        }
        return new WorldRaidLaunchedBattleInfo(remainedPersonages);
    }

    private List<WorldRaidPersonage> materializePersonages(WorldRaidTemplateBattleInfo info) {
        final var result = new ArrayList<WorldRaidPersonage>();
        for (final var template : info.personageTemplatesOrEmpty()) {
            final var personage = template.toPersonage();
            for (int i = 0; i < template.count(); i++) {
                result.add(personage);
            }
        }
        return result;
    }
}
