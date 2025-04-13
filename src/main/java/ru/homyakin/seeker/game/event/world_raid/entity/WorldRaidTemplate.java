package ru.homyakin.seeker.game.event.world_raid.entity;

import ru.homyakin.seeker.locale.Language;

import java.util.Map;

public record WorldRaidTemplate(
    int eventId,
    String code,
    WorldRaidBattleInfo info,
    Map<Language, WorldRaidLocale> locales
) {
}
