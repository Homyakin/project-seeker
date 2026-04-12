package ru.homyakin.seeker.game.group.entity.personage;

import ru.homyakin.seeker.game.online.entity.OnlineType;
import ru.homyakin.seeker.game.online.entity.PersonageLastOnline;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

public record GroupMembersPageResult(
    List<PersonageLastOnline> rows,
    int totalMembers,
    int rangeFrom,
    int rangeTo,
    int totalPages,
    int currentPage,
    Function<Duration, OnlineType> onlineConvertor
) {
}
