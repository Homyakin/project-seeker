package ru.homyakin.seeker.game.group.entity.personage;

import ru.homyakin.seeker.game.personage.models.Personage;

public record GroupMemberDetails(
    Personage personage,
    GroupMemberLastOnline lastOnline
) {
}
