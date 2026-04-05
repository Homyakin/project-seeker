package ru.homyakin.seeker.game.outpost.action;

import io.vavr.control.Either;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.action.GroupTaxService;
import ru.homyakin.seeker.game.group.action.personage.CheckGroupPersonage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.outpost.OutpostBuildingConfig;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.game.outpost.entity.OutpostApplyError;
import ru.homyakin.seeker.game.outpost.entity.OutpostApplyResult;
import ru.homyakin.seeker.game.outpost.entity.OutpostBuildOffer;
import ru.homyakin.seeker.game.outpost.entity.OutpostBuildingProgress;
import ru.homyakin.seeker.game.outpost.entity.OutpostDonateError;
import ru.homyakin.seeker.game.outpost.entity.OutpostDonateSuccess;
import ru.homyakin.seeker.game.outpost.entity.OutpostSlot;
import ru.homyakin.seeker.game.outpost.entity.OutpostSlotAccessError;
import ru.homyakin.seeker.game.outpost.entity.OutpostBuildingContributionStorage;
import ru.homyakin.seeker.game.outpost.entity.OutpostStorage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.shop.ShopConfig;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;

@Component
public class OutpostService {
    private static final int TOP_CONTRIBUTORS_LIMIT = 5;

    private final OutpostStorage storage;
    private final OutpostBuildingContributionStorage contributionStorage;
    private final GroupPersonageStorage groupPersonageStorage;
    private final CheckGroupPersonage checkGroupPersonage;
    private final LockService lockService;
    private final ItemService itemService;
    private final ShopConfig shopConfig;
    private final OutpostBuildingConfig outpostBuildingConfig;
    private final GroupTaxService groupTaxService;
    private final SyncGroupTaxCommand syncGroupTaxCommand;

    public OutpostService(
        OutpostStorage storage,
        OutpostBuildingContributionStorage contributionStorage,
        GroupPersonageStorage groupPersonageStorage,
        CheckGroupPersonage checkGroupPersonage,
        LockService lockService,
        ItemService itemService,
        ShopConfig shopConfig,
        OutpostBuildingConfig outpostBuildingConfig,
        GroupTaxService groupTaxService,
        SyncGroupTaxCommand syncGroupTaxCommand
    ) {
        this.storage = storage;
        this.contributionStorage = contributionStorage;
        this.groupPersonageStorage = groupPersonageStorage;
        this.checkGroupPersonage = checkGroupPersonage;
        this.lockService = lockService;
        this.itemService = itemService;
        this.shopConfig = shopConfig;
        this.outpostBuildingConfig = outpostBuildingConfig;
        this.groupTaxService = groupTaxService;
        this.syncGroupTaxCommand = syncGroupTaxCommand;
    }

    private int materialsRequiredInProgressAfterSync(GroupId groupId, OutpostSlot.BuildingSlot slot) {
        final var targetLevel = slot.level() + 1;
        return outpostBuildingConfig.materialsToReachLevel(
            slot.building(),
            targetLevel,
            groupTaxService.currentTax(groupId)
        );
    }

    public boolean canPersonageBuild(PersonageId personageId) {
        return groupIdIfPersonageCanBuild(personageId).isPresent();
    }

    public Optional<GroupId> groupIdIfPersonageCanBuild(PersonageId personageId) {
        final var member = groupPersonageStorage.getPersonageMemberGroup(personageId);
        if (member.groupId().isEmpty()) {
            return Optional.empty();
        }
        final var groupId = member.groupId().get();
        if (!checkGroupPersonage.isAdminInGroup(groupId, personageId)) {
            return Optional.empty();
        }
        return Optional.of(groupId);
    }

    public List<OutpostSlot> listSlots(GroupId groupId) {
        syncGroupTaxCommand.execute(groupId);
        return listSlotsAfterSync(groupId);
    }

    private List<OutpostSlot> listSlotsAfterSync(GroupId groupId) {
        final var built = storage.listBuildingSlots(groupId);
        final var byBuilding = built.stream()
            .collect(Collectors.toMap(OutpostSlot.BuildingSlot::building, s -> s, (a, _) -> a));
        return Arrays.stream(Building.values())
            .map(b -> {
                final var slot = byBuilding.get(b);
                return slot != null ? enrichMaterialsRequired(groupId, slot) : OutpostSlot.EmptySlot.INSTANCE;
            })
            .toList();
    }

    private OutpostSlot.BuildingSlot enrichMaterialsRequired(GroupId groupId, OutpostSlot.BuildingSlot slot) {
        if (slot.progress().isEmpty()) {
            return new OutpostSlot.BuildingSlot(
                slot.groupId(),
                slot.building(),
                slot.level(),
                slot.progress(),
                0
            );
        }
        final var required = materialsRequiredInProgressAfterSync(groupId, slot);
        return new OutpostSlot.BuildingSlot(
            slot.groupId(),
            slot.building(),
            slot.level(),
            slot.progress(),
            required
        );
    }

    public Either<OutpostSlotAccessError, OutpostSlot> slotForBuilding(PersonageId personageId, Building building) {
        final var member = groupPersonageStorage.getPersonageMemberGroup(personageId);
        if (member.groupId().isEmpty()) {
            return Either.left(OutpostSlotAccessError.NoGroup.INSTANCE);
        }
        final var groupId = member.groupId().get();
        syncGroupTaxCommand.execute(groupId);
        return storage.findBuildingSlot(groupId, building)
            .<Either<OutpostSlotAccessError, OutpostSlot>>map(s -> Either.right(enrichMaterialsRequired(groupId, s)))
            .orElseGet(() -> Either.left(OutpostSlotAccessError.NotFound.INSTANCE));
    }

    public List<OutpostBuildOffer> listBuildOffers(GroupId groupId) {
        syncGroupTaxCommand.execute(groupId);
        return listBuildOffersAfterSync(groupId);
    }

    private List<OutpostBuildOffer> listBuildOffersAfterSync(GroupId groupId) {
        final var taxMultiplier = groupTaxService.currentTax(groupId);
        final var slots = listSlotsAfterSync(groupId);
        final var buildings = Building.values();
        final var offers = new ArrayList<OutpostBuildOffer>();
        for (int i = 0; i < slots.size() && i < buildings.length; i++) {
            final var building = buildings[i];
            switch (slots.get(i)) {
                case OutpostSlot.EmptySlot _ -> offers.add(new OutpostBuildOffer(
                    building,
                    0,
                    1,
                    outpostBuildingConfig.materialsToReachLevel(building, 1, taxMultiplier)
                ));
                case OutpostSlot.BuildingSlot occupied -> {
                    if (occupied.progress().isPresent()) {
                        break;
                    }
                    if (occupied.level() < building.maxLevel()) {
                        final var from = occupied.level();
                        final var to = from + 1;
                        offers.add(new OutpostBuildOffer(
                            building,
                            from,
                            to,
                            outpostBuildingConfig.materialsToReachLevel(building, to, taxMultiplier)
                        ));
                    }
                }
            }
        }
        return List.copyOf(offers);
    }

    public Either<OutpostApplyError, OutpostApplyResult> tryApplyBuildOrUpgrade(PersonageId personageId, Building building) {
        final var member = groupPersonageStorage.getPersonageMemberGroup(personageId);
        if (member.groupId().isEmpty()) {
            return Either.left(OutpostApplyError.NoGroup.INSTANCE);
        }
        final var groupId = member.groupId().get();
        if (!checkGroupPersonage.isAdminInGroup(groupId, personageId)) {
            return Either.left(OutpostApplyError.NotAdmin.INSTANCE);
        }
        syncGroupTaxCommand.execute(groupId);
        final var key = LockPrefixes.OUTPOST.name() + groupId.value();
        return lockService.tryLockAndCalc(
            key,
            () -> applyLocked(groupId, building)
        ).fold(
            k -> Either.left(OutpostApplyError.Busy.INSTANCE),
            inner -> inner
        );
    }

    private Either<OutpostApplyError, OutpostApplyResult> applyLocked(GroupId groupId, Building building) {
        final var offerOpt = listBuildOffersAfterSync(groupId).stream()
            .filter(o -> o.building() == building)
            .findFirst();
        if (offerOpt.isEmpty()) {
            return Either.left(OutpostApplyError.NoOffer.INSTANCE);
        }
        final var offer = offerOpt.get();
        final var progress = OutpostBuildingProgress.started();
        if (offer.fromLevel() == 0) {
            if (!storage.tryInsertWithProgress(groupId, building, 0, progress)) {
                return Either.left(OutpostApplyError.NoOffer.INSTANCE);
            }
        } else {
            if (!storage.trySetProgress(groupId, building, progress)) {
                return Either.left(OutpostApplyError.NoOffer.INSTANCE);
            }
        }
        contributionStorage.clear(groupId, building);
        return Either.right(new OutpostApplyResult(groupId, offer));
    }

    @Transactional
    public Either<OutpostDonateError, OutpostDonateSuccess> tryDonateItemToBuilding(
        PersonageId personageId,
        Building building,
        long itemId
    ) {
        final var member = groupPersonageStorage.getPersonageMemberGroup(personageId);
        if (member.groupId().isEmpty()) {
            return Either.left(OutpostDonateError.NoGroup.INSTANCE);
        }
        final var groupId = member.groupId().get();
        syncGroupTaxCommand.execute(groupId);
        final var key = LockPrefixes.OUTPOST.name() + groupId.value();
        return lockService.<Either<OutpostDonateError, OutpostDonateSuccess>>tryLockAndCalc(
            key,
            () -> donateItemLocked(groupId, personageId, building, itemId)
        ).fold(
            _ -> Either.left(OutpostDonateError.Busy.INSTANCE),
            inner -> inner
        );
    }

    private Either<OutpostDonateError, OutpostDonateSuccess> donateItemLocked(
        GroupId groupId,
        PersonageId personageId,
        Building building,
        long itemId
    ) {
        final var slotOpt = storage.findBuildingSlot(groupId, building);
        if (slotOpt.isEmpty() || slotOpt.get().progress().isEmpty()) {
            return Either.left(OutpostDonateError.BuildingNotInProgress.INSTANCE);
        }
        final var slot = slotOpt.get();
        final var progress = slot.progress().get();
        final var targetLevel = slot.level() + 1;
        final var taxMultiplier = groupTaxService.currentTax(groupId);
        final var materialsRequired = outpostBuildingConfig.materialsToReachLevel(
            building,
            targetLevel,
            taxMultiplier
        );
        final var itemOpt = itemService.getPersonageItem(personageId, itemId);
        if (itemOpt.isEmpty()) {
            return Either.left(OutpostDonateError.ItemNotFound.INSTANCE);
        }
        final var item = itemOpt.get();
        if (item.isEquipped()) {
            return Either.left(OutpostDonateError.ItemEquipped.INSTANCE);
        }
        final var materialsValue = shopConfig.buyingPriceByRarity(item.rarity()).value();
        final var baseDelivered = progress.materialsDelivered();
        final var afterAdd = baseDelivered + materialsValue;
        final var completed = afterAdd >= materialsRequired || baseDelivered >= materialsRequired;
        final var newDelivered = completed ? materialsRequired : Math.min(materialsRequired, afterAdd);

        itemService.removeItem(personageId, itemId);
        contributionStorage.add(groupId, building, personageId, materialsValue);

        if (!completed) {
            if (!storage.updateBuildingProgress(
                groupId,
                building,
                new OutpostBuildingProgress(newDelivered)
            )) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return Either.left(OutpostDonateError.StateConflict.INSTANCE);
            }
            return Either.right(OutpostDonateSuccess.inProgress(
                materialsValue,
                newDelivered,
                materialsRequired
            ));
        }

        final var topContributors = contributionStorage.listTop(groupId, building, TOP_CONTRIBUTORS_LIMIT);
        if (!storage.completeInProgressBuilding(groupId, building)) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Either.left(OutpostDonateError.StateConflict.INSTANCE);
        }
        contributionStorage.clear(groupId, building);
        final var newLevel = slot.level() + 1;
        return Either.right(new OutpostDonateSuccess(
            materialsValue,
            newDelivered,
            materialsRequired,
            true,
            newLevel,
            topContributors
        ));
    }
}
