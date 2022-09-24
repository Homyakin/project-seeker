package ru.homyakin.seeker.event;

import ru.homyakin.seeker.locale.Language;

public record EventLocale(
    Language language,
    String name,
    String description
) {
    public String toMessage() {
        return "*%s*%n%n%s".formatted(
            name,
            description
        );
    }
}
