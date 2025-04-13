package ru.homyakin.seeker.game.event.world_raid.entity;

import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

import java.util.Map;

public record ActiveWorldRaid(
    long id,
    int eventId,
    String code,
    WorldRaidBattleInfo info,
    Money fund,
    ActiveWorldRaidState state,
    Map<Language, WorldRaidLocale> locales
) implements Localized<WorldRaidLocale> {
}
