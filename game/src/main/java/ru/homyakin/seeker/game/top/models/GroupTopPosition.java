package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.badge.entity.BadgeView;

import java.util.Optional;

public interface GroupTopPosition extends TopPosition<GroupId> {
    BadgeView badge();

    Optional<String> tag();

    String name();
}
