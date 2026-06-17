package ru.homyakin.seeker.game.event.models;

import ru.homyakin.seeker.telegram.group.models.GroupTgId;

public record GroupLaunchedEvent(
    long launchedEventId,
    GroupTgId groupId,
    int messageId
) {
}
