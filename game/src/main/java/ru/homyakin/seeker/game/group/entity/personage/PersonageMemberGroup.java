package ru.homyakin.seeker.game.group.entity.personage;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.utils.TimeUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

public record PersonageMemberGroup(
    Optional<GroupId> groupId,
    Optional<LocalDateTime> lastLeaveGroup
) {
    /**
     * @return empty если таймаут истёк или оставшееся время
     */
    public Optional<Duration> remainJoinTimeout(Duration duration) {
        if (lastLeaveGroup.isEmpty()) {
            return Optional.empty();
        }
        final var expireTime = lastLeaveGroup.get().plus(duration);
        final var now = TimeUtils.moscowTime();
        if (now.isAfter(expireTime)) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(now, expireTime));
    }

    public boolean isGroupMember(GroupId groupId) {
        return this.groupId.isPresent() && this.groupId.get().equals(groupId);
    }

    public boolean hasGroup() {
        return groupId.isPresent();
    }
}
