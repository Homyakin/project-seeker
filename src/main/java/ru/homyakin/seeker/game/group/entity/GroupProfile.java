package ru.homyakin.seeker.game.group.entity;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.models.Money;

import java.util.Optional;

public record GroupProfile(
    GroupId id,
    String name,
    Optional<String> tag,
    Money money,
    int memberCount
) {
    public boolean isRegistered() {
        return tag.isPresent();
    }
}
