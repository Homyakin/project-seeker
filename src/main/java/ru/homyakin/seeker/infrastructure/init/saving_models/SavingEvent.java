package ru.homyakin.seeker.infrastructure.init.saving_models;

import java.util.Map;
import ru.homyakin.seeker.game.event.raid.models.RaidLocale;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.event.raid.models.RaidTemplate;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

public record SavingEvent(
    int id,
    boolean isEnabled,
    String code,
    SavingRaid raid
) {
    public void validateLocale() {
        raid.validateLocale();
    }

    public EventType type() {
        return EventType.RAID;
    }

    public record SavingRaid(
        RaidTemplate template,
        Map<Language, RaidLocale> locales
    ) implements Localized<RaidLocale> {}
}
