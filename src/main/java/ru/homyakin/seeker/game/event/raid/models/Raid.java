package ru.homyakin.seeker.game.event.raid.models;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;
import java.util.Map;

public record Raid(
    int eventId,
    String code,
    RaidTemplate template,
    Map<Language, RaidLocale> locales
) implements Localized<RaidLocale> {
}
