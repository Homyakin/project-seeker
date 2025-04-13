package ru.homyakin.seeker.infrastructure.init.saving_models;

import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidBattleInfo;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidLocale;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

import java.util.Map;

public record SavingWorldRaid(
    String code,
    boolean isEnabled,
    WorldRaidBattleInfo info,
    Map<Language, WorldRaidLocale> locales
) implements Localized<WorldRaidLocale> {
}
