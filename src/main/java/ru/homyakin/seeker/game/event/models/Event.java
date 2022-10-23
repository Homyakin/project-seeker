package ru.homyakin.seeker.game.event.models;

import java.time.Duration;
import java.time.Period;
import java.util.List;
import javax.validation.constraints.NotNull;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localization;

public record Event(
    int id,
    @NotNull
    Period period, // для годов-месяцев-дней
    @NotNull
    Duration duration, // для часов-минут-секунд
    @NotNull
    EventType type,
    @NotNull
    List<EventLocale> locales
) {
    public String toStartMessage(Language language) {
        final var locale = getLocaleByLanguageOrDefault(language);
        final var prefixMessage = switch (type) {
            case BOSS -> Localization.get(locale.language()).startBossEvent();
        };

        return "<b>%s: \"%s\"!</b>%n%n%s".formatted(
            prefixMessage,
            locale.name(),
            locale.description()
        );
    }

    public String endMessage(Language language, EventResult result) {
        final var postfix = switch (type) {
            case BOSS -> bossEndMessage(language, result);
        };
        return Localization.get(language).expiredEvent() + " " + postfix;
    }

    private String bossEndMessage(Language language, EventResult result) {
        if (result instanceof EventResult.Success) {
            return Localization.get(language).successBoss();
        } else if (result instanceof EventResult.Failure) {
            return Localization.get(language).failureBoss();
        }
        return "";
    }

    private EventLocale getLocaleByLanguageOrDefault(Language language) {
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
