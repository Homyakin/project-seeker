package ru.homyakin.seeker.game.event.models;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localization;

public record EventLocale(
    Language language,
    String name,
    String description
) {
    public String toStartMessage() {
        return "<b>%s</b>%n%n%s".formatted(
            name,
            description
        );
    }

    public String toEndMessage() {
        return toStartMessage() + "\n\n" + Localization.get(language).expiredEvent();
    }
}
