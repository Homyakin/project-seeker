package ru.homyakin.seeker.game.personage.power;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.locale.LocaleUtils;

/**
 * Explicitly gated display conversion. Nothing in combat, matchmaking or rewards consumes this service.
 */
@Service
public class PowerDisplayService {
    private final PowerDisplayVersion version;

    public PowerDisplayService(
        @Value("${homyakin.seeker.power-display.version:LEGACY_V1}") PowerDisplayVersion version
    ) {
        this.version = java.util.Objects.requireNonNull(version);
    }

    public PowerDisplayVersion version() {
        return version;
    }

    public int value(List<Item> items, BattlePersonage legacyPersonage) {
        return switch (version) {
            case LEGACY_V1 -> LocaleUtils.legacyPowerForDisplay((int) legacyPersonage.legacyPower());
            case REFERENCE_V1 -> version.referenceValue(items);
        };
    }

    public int value(List<Item> items) {
        if (version == PowerDisplayVersion.REFERENCE_V1) {
            return version.referenceValue(items);
        }
        return value(items, new BattlePersonage(items, Position.FRONT));
    }

    public Optional<String> battleReportValue(Optional<PowerDisplaySnapshot> snapshot) {
        if (version != PowerDisplayVersion.REFERENCE_V1) {
            return Optional.empty();
        }
        return snapshot.filter(value -> value.version() == version).map(value -> Integer.toString(value.value()));
    }
}
