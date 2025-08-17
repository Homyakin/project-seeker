package ru.homyakin.seeker.game.event.raid.models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.event.raid.generator.GroupGenerator;
import ru.homyakin.seeker.game.event.raid.generator.RaidBattlePersonageGenerator;
import ru.homyakin.seeker.game.event.raid.generator.SingleBossGenerator;

public enum RaidTemplate {
    SINGLE_BOSS(1, new SingleBossGenerator()),
    ENEMY_GROUP(2, new GroupGenerator()),
    ;

    private final int id;
    private final RaidBattlePersonageGenerator generator;

    RaidTemplate(int id, RaidBattlePersonageGenerator generator) {
        this.id = id;
        this.generator = generator;
    }

    public int id() {
        return id;
    }

    private static final Map<Integer, RaidTemplate> map = new HashMap<>() {{
        Arrays.stream(RaidTemplate.values()).forEach(it -> put(it.id, it));
    }};

    public static RaidTemplate get(int id) {
        return Optional.ofNullable(map.get(id))
            .orElseThrow(() -> new IllegalStateException("Unexpected raid template id: " + id));
    }

    public List<BattlePersonage> generate(List<BattlePersonage> personages, double powerPercent) {
        return generator.generate(personages.size(), powerPercent);
    }
}
