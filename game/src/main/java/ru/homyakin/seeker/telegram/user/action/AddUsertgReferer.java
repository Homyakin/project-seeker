package ru.homyakin.seeker.telegram.user.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.user.entity.UsertgRefererStorage;
import ru.homyakin.seeker.telegram.user.models.UserId;

import java.time.ZonedDateTime;

@Component
public class AddUsertgReferer {
    private final UsertgRefererStorage storage;

    public AddUsertgReferer(UsertgRefererStorage storage) {
        this.storage = storage;
    }

    public void addReferer(UserId userId, String referer, ZonedDateTime dateTime) {
        storage.saveReferer(userId, referer, dateTime);
    }
}
