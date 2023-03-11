package ru.homyakin.seeker.game.event.models;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.raid.RaidLocalization;

public record EventLocale(
    Language language,
    String intro,
    String description
) {
    public String toStartMessage() {
        return "<b>%s</b>%n%n%s".formatted(
            intro,
            description
        );
    }

    public String toEndMessage() {
        return toStartMessage() + "\n\n" + RaidLocalization.expiredRaid(language);
    }
}
