package ru.homyakin.seeker.game.event.anomaly.entity;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;

public interface SendAnomalyChallengeToGroup {
    void send(GroupId groupId, LaunchedEvent challengedEvent, LaunchedEvent searchingEvent);
}
