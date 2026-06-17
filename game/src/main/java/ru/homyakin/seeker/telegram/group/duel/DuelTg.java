package ru.homyakin.seeker.telegram.group.duel;

import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record DuelTg(
    long duelId,
    GroupTgId groupTgId,
    int messageId
) {
}
