package ru.homyakin.seeker.infrastructure.init.saving_models;

import java.util.Map;
import java.util.Optional;
import ru.homyakin.seeker.game.event.models.EventLocale;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.event.raid.models.RaidTemplate;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

public record SavingEvent(
    int id,
    boolean isEnabled,
    Map<Language, EventLocale> locales,
    Optional<SavingRaid> raid
) implements Localized<EventLocale> {
    public EventType type() {
        if (raid.isPresent()) {
            return EventType.RAID;
        }
        throw new IllegalStateException("Event type must present");
    }

    public record SavingRaid(
        String name,
        RaidTemplate template
    ) {}
}
