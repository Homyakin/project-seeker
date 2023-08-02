package ru.homyakin.seeker.game.event.models;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import ru.homyakin.seeker.game.event.raid.RaidResult;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.raid.RaidLocalization;
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

    public String endMessage(Language language, RaidResult raidResult) {
        return switch (type) {
            case RAID -> raidEndMessage(language, raidResult);
        };
    }

    private Optional<String> endDateText(Language language, LocalDateTime endDate) {
        final var now = TimeUtils.moscowTime();
        if (endDate.isBefore(now)) {
            return Optional.empty();
        }
        final var diff = Duration.between(now, endDate);
        var hours = "";
        if (diff.toHours() > 0) {
            hours = diff.toHours() + " " + RaidLocalization.hoursShort(language);
        }
        var minutes = "";
        if (diff.toMinutesPart() > 0) {
            minutes = diff.toMinutesPart() + " " + RaidLocalization.minutesShort(language);
        } else if (diff.toHours() == 0) {
            minutes = "1 " + RaidLocalization.minutesShort(language);
        }
        final var prefix = switch (type) {
            case RAID -> RaidLocalization.raidStartsPrefix(language);
        };
        return Optional.of(prefix + hours + " " + minutes);
    }

    private String raidEndMessage(Language language, RaidResult raidResult) {
        if (raidResult.isSuccess()) {
            return RaidLocalization.successRaid(language) + "\n\n" + RaidLocalization.raidResult(language, raidResult);
        } else {
            return RaidLocalization.failureRaid(language) + "\n\n" + RaidLocalization.raidResult(language, raidResult);
        }
    }

    private EventLocale getLocaleByLanguageOrDefault(Language language) {
        return LocaleUtils.getLocaleByLanguageOrDefault(locales, language)
            .orElseThrow(() -> new IllegalStateException("No locales for event " + id));
    }
}
