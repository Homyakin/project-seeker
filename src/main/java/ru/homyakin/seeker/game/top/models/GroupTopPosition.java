package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.common.models.GroupId;

import java.util.Optional;

public interface GroupTopPosition extends TopPosition<GroupId> {
    Optional<String> tag();

    String name();
}
