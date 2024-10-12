package ru.homyakin.seeker.telegram.user.entity;

import ru.homyakin.seeker.telegram.user.models.UserId;

import java.time.ZonedDateTime;

public interface UsertgRefererStorage {
    void saveReferer(UserId userId, String referer, ZonedDateTime dateTime);
}
