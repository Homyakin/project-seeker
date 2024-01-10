package ru.homyakin.seeker.game.event.models;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import jakarta.validation.constraints.NotNull;
import ru.homyakin.seeker.game.event.raid.models.RaidResult;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.raid.RaidLocalization;

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
    private String toBaseMessage(Language language) {
        final var locale = getLocaleByLanguageOrDefault(language);

        return "<b>%s</b>%n%n%s".formatted(
            locale.intro(),
            locale.description()
        );
    }

    public String toEndMessage(Language language, List<Personage> participants) {
        if (participants.isEmpty()) {
            return toBaseMessage(language);
        } else {
            return toBaseMessage(language) + "\n\n" + RaidLocalization.raidParticipants(language, participants);
        }
    }

    public String toStartMessage(Language language, LocalDateTime startDate, LocalDateTime endDate) {
        final var endDateText = endDateText(language, startDate, endDate);
        return endDateText
            .map(s -> toBaseMessage(language) + "\n\n" + s)
            .orElseGet(() -> toBaseMessage(language));
    }

    public String endMessage(Language language, RaidResult raidResult) {
        return switch (type) {
            case RAID -> raidEndMessage(language, raidResult);
        };
    }

    private Optional<String> endDateText(Language language, LocalDateTime startDate, LocalDateTime endDate) {
        if (endDate.isBefore(startDate)) {
            return Optional.empty();
        }
        final var diff = Duration.between(startDate, endDate);
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
        return Optional.of(prefix + " " + hours + " " + minutes);
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
