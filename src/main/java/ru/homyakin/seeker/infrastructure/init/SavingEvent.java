package ru.homyakin.seeker.infrastructure.init;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import ru.homyakin.seeker.game.event.models.EventLocale;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.event.raid.models.RaidTemplate;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Localized;

public record SavingEvent(
    int id,
    Duration duration,
    boolean isEnabled,
    List<EventLocale> locales,
    Optional<SavindRaid> raid
) implements Localized<EventLocale> {

    public void validateLocale() {
        if (!LocaleUtils.checkDefaultLanguage(locales)) {
            throw new IllegalStateException("Locale must have default language " + Language.DEFAULT + " at event " + id);
        }
    }

    public EventType type() {
        if (raid.isPresent()) {
            return EventType.RAID;
        }
        throw new IllegalStateException("Event type must present");
    }

    public record SavindRaid(
        String name,
        RaidTemplate template
    ) {}
}
