package ru.homyakin.seeker.game.event.raid.models;

import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.personage.event.RaidParticipant;
import ru.homyakin.seeker.game.personage.models.PersonageRaidResult;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.raid.RaidLocalization;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record Raid(
    int eventId,
    String code,
    RaidTemplate template,
    Map<Language, RaidLocale> locales
) implements Localized<RaidLocale> {
    private String toBaseMessage(Language language) {
        final var locale = getLocaleOrDefault(language);
        // TODO в локализацию
        return "<b>%s</b>%n%n%s".formatted(
            locale.intro(),
            locale.description()
        );
    }

    public String toEndMessage(EventResult.RaidResult.Completed result, Language language) {
        final var participants = result.personageResults().stream()
            .map(PersonageRaidResult::participant)
            .toList();
        return toEndMessageWithParticipants(participants, language);
    }

    public String toEndMessageWithParticipants(List<RaidParticipant> participants, Language language) {
        return toBaseMessage(language) + "\n\n" + RaidLocalization.raidParticipants(language, participants);
    }

    public String toStartMessage(Language language, LocalDateTime startDate, LocalDateTime endDate, int raidLevel) {
        final var endDateText = endDateText(language, startDate, endDate);
        final var baseMessage = toBaseMessage(language);
        final var levelText = "\n\n" + RaidLocalization.raidLevel(language, raidLevel);
        return endDateText
            .map(s -> baseMessage + levelText + "\n\n" + s)
            .orElseGet(() -> baseMessage + levelText);
    }

    public String endMessage(Language language, EventResult.RaidResult.Completed result) {
        if (result.status() == EventResult.RaidResult.Completed.Status.SUCCESS) {
            return RaidLocalization.successRaid(language) + "\n\n" + RaidLocalization.raidResult(language, result);
        } else {
            return RaidLocalization.failureRaid(language) + "\n\n" + RaidLocalization.raidResult(language, result);
        }
    }

    private Optional<String> endDateText(Language language, LocalDateTime startDate, LocalDateTime endDate) {
        final String duration;
        if (startDate.isBefore(endDate)) {
            duration = CommonLocalization.duration(language, startDate, endDate);
        } else {
            return Optional.empty();
        }
        final var prefix = RaidLocalization.raidStartsPrefix(language);
        return Optional.of(prefix + " " + duration);
    }
}
