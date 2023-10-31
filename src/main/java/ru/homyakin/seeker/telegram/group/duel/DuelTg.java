package ru.homyakin.seeker.telegram.group.duel;

import ru.homyakin.seeker.telegram.group.models.GroupId;

public record DuelTg(
    long duelId,
    GroupId groupTgId,
    int messageId
) {
}
