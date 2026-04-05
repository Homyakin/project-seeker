package ru.homyakin.seeker.game.outpost.action;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.action.GroupTaxService;
import ru.homyakin.seeker.game.outpost.OutpostBuildingConfig;
import ru.homyakin.seeker.game.outpost.entity.OutpostBuildingCompletion;
import ru.homyakin.seeker.game.outpost.entity.OutpostBuildingContributionStorage;
import ru.homyakin.seeker.game.outpost.entity.OutpostStorage;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;

@Component
public class SyncGroupTaxCommand {
    private static final int TOP_CONTRIBUTORS_LIMIT = 5;
    private static final Logger log = LoggerFactory.getLogger(SyncGroupTaxCommand.class);
    private final GroupTaxService groupTaxService;
    private final LockService lockService;
    private final TransactionTemplate transactionTemplate;
    private final OutpostStorage storage;
    private final OutpostBuildingConfig outpostBuildingConfig;
    private final OutpostBuildingContributionStorage contributionStorage;
    private final OutpostGroupBuildingCompletedNotifier notifier;

    public SyncGroupTaxCommand(
        GroupTaxService groupTaxService,
        LockService lockService,
        PlatformTransactionManager transactionManager,
        OutpostStorage storage,
        OutpostBuildingConfig outpostBuildingConfig,
        OutpostBuildingContributionStorage contributionStorage,
        OutpostGroupBuildingCompletedNotifier notifier
    ) {
        this.groupTaxService = groupTaxService;
        this.lockService = lockService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.storage = storage;
        this.outpostBuildingConfig = outpostBuildingConfig;
        this.contributionStorage = contributionStorage;
        this.notifier = notifier;
    }

    public void execute(GroupId groupId) {
        groupTaxService.recalculateTax(groupId);
        final var key = LockPrefixes.OUTPOST.name() + groupId.value();
        lockService.tryLockAndCalc(
            key,
            () -> transactionTemplate.execute(_ -> completeInProgressBuildingsIfSaturated(groupId))
        ).peek(
            list -> notifier.notifyGroup(groupId, list)
        );
    }

    private List<OutpostBuildingCompletion> completeInProgressBuildingsIfSaturated(GroupId groupId) {
        final var completions = new ArrayList<OutpostBuildingCompletion>();
        final int taxMultiplier = groupTaxService.currentTax(groupId);
        final var slots = storage.listBuildingSlots(groupId);
        for (final var slot : slots) {
            if (slot.progress().isEmpty()) {
                continue;
            }
            final var building = slot.building();
            final var targetLevel = slot.level() + 1;
            final var required = outpostBuildingConfig.materialsToReachLevel(
                building,
                targetLevel,
                taxMultiplier
            );
            if (slot.progress().get().materialsDelivered() < required) {
                continue;
            }
            final var topContributors = contributionStorage.listTop(groupId, building, TOP_CONTRIBUTORS_LIMIT);
            if (storage.completeInProgressBuilding(groupId, building)) {
                log.info("Completed building at tax sync, {}, {}", groupId, building);
                contributionStorage.clear(groupId, building);
                completions.add(new OutpostBuildingCompletion(building, targetLevel, topContributors));
            }
        }
        return completions;
    }
}
