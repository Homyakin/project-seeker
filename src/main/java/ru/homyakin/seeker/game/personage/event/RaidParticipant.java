package ru.homyakin.seeker.game.personage.event;

import ru.homyakin.seeker.game.event.raid.models.RaidPersonageParams;
import ru.homyakin.seeker.game.personage.models.Personage;

public record RaidParticipant(
    Personage personage,
    RaidPersonageParams params
) {
}
