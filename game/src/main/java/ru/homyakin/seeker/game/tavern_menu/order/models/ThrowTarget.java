package ru.homyakin.seeker.game.tavern_menu.order.models;

import ru.homyakin.seeker.game.personage.models.Personage;

public sealed interface ThrowTarget {

    record PersonageTarget(
        Personage personage
    ) implements ThrowTarget {
    }

    enum TavernStaff implements ThrowTarget {
        INSTANCE
    }

    enum None implements ThrowTarget {
        INSTANCE
    }
}
