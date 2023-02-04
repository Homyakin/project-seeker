package ru.homyakin.seeker.game.event.raid;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.event.raid.generator.RaidBattleGenerator;
import ru.homyakin.seeker.game.event.raid.generator.SingleBossGenerator;

public enum RaidTemplate {
    SINGLE_BOSS(1, new SingleBossGenerator()),
    ;

    private final int id;
    private final RaidBattleGenerator generator;

    RaidTemplate(int id, RaidBattleGenerator generator) {
        this.id = id;
        this.generator = generator;
    }

    private static final Map<Integer, RaidTemplate> map = new HashMap<>() {{
        Arrays.stream(RaidTemplate.values()).forEach(it -> put(it.id, it));
    }};

    public static RaidTemplate get(int id) {
        return Optional.ofNullable(map.get(id))
            .orElseThrow(() -> new IllegalStateException("Unexpected raid template id: " + id));
    }

    public List<BattlePersonage> generate() {
        return switch (this) {
            case SINGLE_BOSS -> generator.generate();
        };
    }
}
