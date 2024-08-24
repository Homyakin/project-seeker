package ru.homyakin.seeker.infrastructure.init.saving_models;

import java.util.Map;
import ru.homyakin.seeker.game.event.raid.models.RaidLocale;
import ru.homyakin.seeker.game.event.raid.models.RaidTemplate;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

public record SavingRaid(
    boolean isEnabled,
    String code,
    RaidTemplate template,
    Map<Language, RaidLocale> locales
) implements Localized<RaidLocale> {
}
