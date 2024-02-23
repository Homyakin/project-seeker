package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.game.personage.badge.BadgeView;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;

public interface TopPosition {
    PersonageId personageId();

    String personageName();

    BadgeView personageBadge();

    default String personageBadgeWithName() {
        return personageBadge().icon() + personageName();
    }

    int score();

    String toLocalizedString(Language language, int positionNumber);
}
