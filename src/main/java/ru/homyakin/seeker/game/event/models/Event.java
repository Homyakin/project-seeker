package ru.homyakin.seeker.game.event.models;

import java.time.Duration;
import java.time.Period;
import java.util.List;
import javax.validation.constraints.NotNull;
import ru.homyakin.seeker.locale.Language;

public record Event(
    int id,
    @NotNull
    Period period, // для годов-месяцев-дней
    @NotNull
    Duration duration, // для часов-минут-секунд
    @NotNull
    List<EventLocale> locales
) {
    public EventLocale getLocaleByLanguageOrDefault(Language language) {
        var result = locales.stream().filter(locale -> locale.language() == language).findFirst();
        if (result.isPresent()) {
            return result.get();
        }
        result = locales.stream().filter(locale -> locale.language() == Language.DEFAULT).findFirst();
        if (result.isPresent()) {
            return result.get();
        }
        return locales.stream().findFirst().orElseThrow(() -> new IllegalStateException("No locales for event " + id));
    }
}
