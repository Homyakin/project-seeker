package ru.homyakin.seeker.game.online;

import ru.homyakin.seeker.game.online.entity.OnlineType;

import java.time.Duration;

public interface OnlineTypeProvider {
    OnlineType convertDuration(Duration duration);
}
