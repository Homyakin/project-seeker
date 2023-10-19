package ru.homyakin.seeker.game.event.models;

import ru.homyakin.seeker.telegram.group.models.GroupId;

public record GroupLaunchedEvent(
    long launchedEventId,
    GroupId groupId,
    int messageId
) {
}
