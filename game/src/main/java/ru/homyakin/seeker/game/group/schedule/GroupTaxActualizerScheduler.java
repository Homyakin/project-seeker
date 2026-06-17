package ru.homyakin.seeker.game.group.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ru.homyakin.seeker.game.group.entity.GroupConfig;
import ru.homyakin.seeker.game.group.entity.GroupTaxStorage;
import ru.homyakin.seeker.game.outpost.action.SyncGroupTaxCommand;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class GroupTaxActualizerScheduler {
    private static final Logger logger = LoggerFactory.getLogger(GroupTaxActualizerScheduler.class);

    private final GroupTaxStorage taxStorage;
    private final GroupConfig groupConfig;
    private final SyncGroupTaxCommand syncGroupTaxCommand;

    public GroupTaxActualizerScheduler(
        GroupTaxStorage taxStorage,
        GroupConfig groupConfig,
        SyncGroupTaxCommand syncGroupTaxCommand
    ) {
        this.taxStorage = taxStorage;
        this.groupConfig = groupConfig;
        this.syncGroupTaxCommand = syncGroupTaxCommand;
    }

    @Scheduled(fixedRate = 2000)
    public void actualize() {
        final var cutoff = TimeUtils.moscowTime().minus(groupConfig.groupTaxRecalcInterval());
        for (final var groupId : taxStorage.findGroupIdsDueForTaxUpdate(cutoff)) {
            try {
                syncGroupTaxCommand.execute(groupId);
            } catch (Exception e) {
                logger.warn("Group tax decay failed for {}", groupId, e);
            }
        }
    }
}
