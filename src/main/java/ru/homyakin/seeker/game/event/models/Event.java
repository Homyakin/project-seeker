package ru.homyakin.seeker.game.event.models;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.utils.TimeUtils;

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

        return "<b>%s</b>%n%n%s".formatted(
            locale.intro(),
            locale.description()
        );
    }

    public String toStartMessage(Language language, LocalDateTime endDate) {
        final var locale = getLocaleByLanguageOrDefault(language);
        final var endDateText = endDateText(language, endDate);
        if (endDateText.isEmpty()) {
            return toStartMessage(language);
        }
        return
            """
                <b>%s</b>
                                
                %s
                                
                %s
                """.formatted(
                locale.intro(),
                locale.description(),
                endDateText.get()
            );
    }

    public String endMessage(Language language, EventResult result) {
        final var postfix = switch (type) {
            case BOSS -> bossEndMessage(language, result);
        };
        return Localization.get(language).expiredEvent() + " " + postfix;
    }

    private Optional<String> endDateText(Language language, LocalDateTime endDate) {
        final var now = TimeUtils.moscowTime();
        if (endDate.isBefore(now)) {
            return Optional.empty();
        }
        final var diff = Duration.between(now, endDate);
        var hours = "";
        if (diff.toHours() > 0) {
            hours = diff.toHours() + " " + Localization.get(language).hoursShort();
        }
        var minutes = "";
        if (diff.toMinutesPart() > 0) {
            minutes = diff.toMinutesPart() + " " + Localization.get(language).minutesShort();
        } else if (diff.toHours() == 0) {
            minutes = "1 " + Localization.get(language).minutesShort();
        }
        final var prefix = switch (type) {
            case BOSS -> Localization.get(language).bossBattleStarts();
        };
        return Optional.of(prefix + hours + " " + minutes);
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
