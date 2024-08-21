package ru.homyakin.seeker.game.event.models;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.validation.constraints.NotNull;
import ru.homyakin.seeker.game.event.raid.models.RaidResult;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.raid.RaidLocalization;

public record Event(
    int id,
    @NotNull
    EventType type,
    @NotNull
    Map<Language, EventLocale> locales
) implements Localized<EventLocale> {
    private String toBaseMessage(Language language) {
        final var locale = getLocaleOrDefault(language);
        // TODO в локализацию
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
        final String duration;
        if (startDate.isBefore(endDate)) {
            duration = CommonLocalization.duration(language, startDate, endDate);
        } else {
            return Optional.empty();
        }
        final var prefix = switch (type) {
            case RAID -> RaidLocalization.raidStartsPrefix(language);
        };
        return Optional.of(prefix + " " + duration);
    }

    private String raidEndMessage(Language language, RaidResult raidResult) {
        if (raidResult.isSuccess()) {
            return RaidLocalization.successRaid(language) + "\n\n" + RaidLocalization.raidResult(language, raidResult);
        } else {
            return RaidLocalization.failureRaid(language) + "\n\n" + RaidLocalization.raidResult(language, raidResult);
        }
    }
}
