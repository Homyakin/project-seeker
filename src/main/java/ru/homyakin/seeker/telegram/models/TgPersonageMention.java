package ru.homyakin.seeker.telegram.models;

import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.PersonageMention;
import ru.homyakin.seeker.telegram.user.models.UserId;

public class TgPersonageMention implements PersonageMention {
    private final String value;

    private TgPersonageMention(String value) {
        this.value = value;
    }

    public static TgPersonageMention of(Personage personage, UserId userId) {
        return new TgPersonageMention(
            "<a href=\"tg://user?id=" + userId.value() + "\">" + personage.iconWithName() + "</a>"
        );
    }

    @Override
    public String value() {
        return value;
    }
}
