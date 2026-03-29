package ru.homyakin.seeker.game.outpost.action;

import io.vavr.control.Either;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.action.personage.CheckGroupPersonage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.game.outpost.entity.OutpostBuildingProgress;
import ru.homyakin.seeker.game.outpost.entity.OutpostApplyError;
import ru.homyakin.seeker.game.outpost.entity.OutpostApplyResult;
import ru.homyakin.seeker.game.outpost.entity.OutpostBuildOffer;
import ru.homyakin.seeker.game.outpost.entity.OutpostSlot;
import ru.homyakin.seeker.game.outpost.entity.OutpostStorage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;

@Component
public class OutpostService {
    private final OutpostStorage storage;
    private final GroupPersonageStorage groupPersonageStorage;
    private final CheckGroupPersonage checkGroupPersonage;
    private final LockService lockService;

    public OutpostService(
        OutpostStorage storage,
        GroupPersonageStorage groupPersonageStorage,
        CheckGroupPersonage checkGroupPersonage,
        LockService lockService
    ) {
        this.storage = storage;
        this.groupPersonageStorage = groupPersonageStorage;
        this.checkGroupPersonage = checkGroupPersonage;
        this.lockService = lockService;
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
        final var built = storage.listBuildingSlots(groupId);
        final var byBuilding = built.stream()
            .collect(Collectors.toMap(OutpostSlot.BuildingSlot::building, s -> s, (a, _) -> a));
        return Arrays.stream(Building.values())
            .map(b -> {
                final var slot = byBuilding.get(b);
                return slot != null ? slot : OutpostSlot.EmptySlot.INSTANCE;
            })
            .toList();
    }

    public OutpostSlot slotForBuilding(GroupId groupId, Building building) {
        return storage.findBuildingSlot(groupId, building)
            .<OutpostSlot>map(Function.identity())
            .orElse(OutpostSlot.EmptySlot.INSTANCE);
    }

    public List<OutpostBuildOffer> listBuildOffers(GroupId groupId) {
        final var slots = listSlots(groupId);
        final var buildings = Building.values();
        final var offers = new ArrayList<OutpostBuildOffer>();
        for (int i = 0; i < slots.size() && i < buildings.length; i++) {
            final var building = buildings[i];
            switch (slots.get(i)) {
                case OutpostSlot.EmptySlot _ -> offers.add(new OutpostBuildOffer(building, 0, 1));
                case OutpostSlot.BuildingSlot occupied -> {
                    if (occupied.progress().isPresent()) {
                        break;
                    }
                    if (occupied.level() < building.maxLevel()) {
                        final var from = occupied.level();
                        offers.add(new OutpostBuildOffer(building, from, from + 1));
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
        final var key = LockPrefixes.OUTPOST.name() + groupId.value();
        return lockService.<Either<OutpostApplyError, OutpostApplyResult>>tryLockAndCalc(
            key,
            () -> applyLocked(groupId, building)
        ).fold(
            k -> Either.left(OutpostApplyError.Busy.INSTANCE),
            inner -> inner
        );
    }

    private Either<OutpostApplyError, OutpostApplyResult> applyLocked(GroupId groupId, Building building) {
        final var offerOpt = listBuildOffers(groupId).stream()
            .filter(o -> o.building() == building)
            .findFirst();
        if (offerOpt.isEmpty()) {
            return Either.left(OutpostApplyError.NoOffer.INSTANCE);
        }
        final var offer = offerOpt.get();
        final var materialsRequired = building.materialsToReachLevel(offer.toLevel());
        final var progress = OutpostBuildingProgress.started(materialsRequired);
        if (offer.fromLevel() == 0) {
            if (!storage.tryInsertWithProgress(groupId, building, 0, progress)) {
                return Either.left(OutpostApplyError.NoOffer.INSTANCE);
            }
        } else {
            if (!storage.trySetProgress(groupId, building, progress)) {
                return Either.left(OutpostApplyError.NoOffer.INSTANCE);
            }
        }
        return Either.right(new OutpostApplyResult(groupId, offer));
    }
}
