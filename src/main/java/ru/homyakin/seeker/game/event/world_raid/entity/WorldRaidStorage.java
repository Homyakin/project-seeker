package ru.homyakin.seeker.game.event.world_raid.entity;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingWorldRaid;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface WorldRaidStorage {
    void save(int eventId, SavingWorldRaid raid);

    Optional<ActiveWorldRaid> getActive();

    Optional<WorldRaidTemplate> getRandom();

    void saveActive(
        WorldRaidTemplate worldRaidTemplate,
        Money fund,
        ActiveWorldRaidState.Research research
    );

    void addContribution(
        long id,
        PersonageId personageId,
        Money rewardIncrease,
        int contribution
    );

    void setBattleState(long id, long launchedEventId);

    void saveAsContinued(
        ActiveWorldRaid raid,
        WorldRaidBattleInfo info,
        ActiveWorldRaidState.Research research
    );

    void setStatus(long id, FinalWorldRaidStatus status);

    List<PersonageContribution> getPersonageContributions(long id);

    void setRewards(long id, Map<PersonageId, Money> rewards);

    void updateFund(long id, Money fund);

    List<GroupId> getRegisteredGroupsToNotify(long worldRaidId, Duration timeout);

    void updateGroupNotification(long worldRaidId, GroupId groupId, LocalDateTime lastNotification);

    Optional<Long> getLaunchedEventIdForLastFinished();
}
