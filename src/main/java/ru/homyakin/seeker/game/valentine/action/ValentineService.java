package ru.homyakin.seeker.game.valentine.action;

import org.springframework.stereotype.Service;

import io.vavr.control.Either;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.badge.action.BadgeService;
import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.badge.entity.PersonageBadgeStorage;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.game.group.action.personage.ActiveGroupPersonagesService;
import ru.homyakin.seeker.game.group.action.personage.RandomGroupPersonage;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.valentine.entity.ValentineConfig;
import ru.homyakin.seeker.game.valentine.entity.ValentineError;
import ru.homyakin.seeker.game.valentine.entity.ValentineResult;
import ru.homyakin.seeker.game.valentine.entity.ValentineStorage;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class ValentineService {
    private final ValentineStorage storage;
    private final ValentineConfig config;
    private final PersonageService personageService;
    private final GetGroup getGroup;
    private final RandomGroupPersonage randomGroupPersonage;
    private final ActiveGroupPersonagesService activeGroupPersonagesService;
    private final BadgeService badgeService;
    private final PersonageBadgeStorage personageBadgeStorage;

    public ValentineService(
        ValentineStorage storage,
        ValentineConfig config,
        PersonageService personageService,
        GetGroup getGroup,
        RandomGroupPersonage randomGroupPersonage,
        ActiveGroupPersonagesService activeGroupPersonagesService,
        BadgeService badgeService,
        PersonageBadgeStorage personageBadgeStorage
    ) {
        this.storage = storage;
        this.config = config;
        this.personageService = personageService;
        this.getGroup = getGroup;
        this.randomGroupPersonage = randomGroupPersonage;
        this.activeGroupPersonagesService = activeGroupPersonagesService;
        this.badgeService = badgeService;
        this.personageBadgeStorage = personageBadgeStorage;
    }

    public Either<ValentineError, ValentineResult> sendToSameGroup(
        PersonageId senderId,
        PersonageId receiverId,
        GroupId groupId
    ) {
        final var group = getGroup.forceGet(groupId);
        if (!group.isRegistered()) {
            return Either.left(ValentineError.NotRegisteredGroup.INSTANCE);
        }

        final var sender = personageService.getByIdForce(senderId);
        if (!sender.isGroupMember()) {
            return Either.left(ValentineError.NotGroupMember.INSTANCE);
        }

        if (senderId.equals(receiverId)) {
            return Either.left(ValentineError.CannotSendToSelf.INSTANCE);
        }

        final var receiver = personageService.getByIdForce(receiverId);
        if (!receiver.isGroupMember()) {
            return Either.left(ValentineError.ReceiverNotRegistered.INSTANCE);
        }

        final boolean inSameGroup = sender.isSameGroup(receiver);
        final var goldCost = inSameGroup ? config.sameGroupGold() : config.otherGroupGold();
        final var energyCost = inSameGroup ? config.sameGroupEnergyCost() : config.otherGroupEnergyCost();

        return chargeAndSend(sender, receiver, goldCost, energyCost, false, groupId, groupId)
            .map(pair -> new ValentineResult.SameGroup(
                pair.sender(),
                pair.receiver(),
                goldCost,
                energyCost,
                pair.senderBadge(),
                pair.receiverBadge()
            ));
    }

    public Either<ValentineError, ValentineResult> sendToOtherGroup(
        PersonageId senderId,
        GroupId senderGroupId,
        String targetTag,
        PersonageId receiverId
    ) {
        final var senderGroup = getGroup.forceGet(senderGroupId);
        if (!senderGroup.isRegistered()) {
            return Either.left(ValentineError.NotRegisteredGroup.INSTANCE);
        }

        final var sender = personageService.getByIdForce(senderId);
        if (!sender.isGroupMember(senderGroup)) {
            return Either.left(ValentineError.NotGroupMember.INSTANCE);
        }

        final var targetGroupResult = getAndValidateTargetGroup(senderGroup, targetTag);
        if (targetGroupResult.isLeft()) {
            return Either.left(targetGroupResult.getLeft());
        }
        final var targetGroup = targetGroupResult.get();

        if (senderId.equals(receiverId)) {
            return Either.left(ValentineError.CannotSendToSelf.INSTANCE);
        }

        final var receiver = personageService.getByIdForce(receiverId);
        if (!receiver.isGroupMember()) {
            return Either.left(ValentineError.ReceiverNotRegistered.INSTANCE);
        }
        if (!receiver.isGroupMember(targetGroup)) {
            return Either.left(ValentineError.ReceiverNotInTargetGroup.INSTANCE);
        }
        if (!activeGroupPersonagesService.isPersonageActiveInGroup(targetGroup.id(), receiverId)) {
            return Either.left(ValentineError.ReceiverNotActiveInGroup.INSTANCE);
        }

        final var goldCost = config.otherGroupGold();
        final var energyCost = config.otherGroupEnergyCost();

        return chargeAndSend(sender, receiver, goldCost, energyCost, false, senderGroupId, targetGroup.id())
            .map(pair -> new ValentineResult.OtherGroup(
                pair.sender(),
                pair.receiver(),
                senderGroup,
                targetGroup,
                goldCost,
                energyCost,
                pair.senderBadge(),
                pair.receiverBadge()
            ));
    }

    public Either<ValentineError, ValentineResult> sendToRandomInGroup(
        PersonageId senderId,
        GroupId senderGroupId,
        String targetTag
    ) {
        final var senderGroup = getGroup.forceGet(senderGroupId);
        if (!senderGroup.isRegistered()) {
            return Either.left(ValentineError.NotRegisteredGroup.INSTANCE);
        }

        final var sender = personageService.getByIdForce(senderId);
        if (!sender.isGroupMember(senderGroup)) {
            return Either.left(ValentineError.NotGroupMember.INSTANCE);
        }

        final var targetGroupResult = getAndValidateTargetGroup(senderGroup, targetTag);
        if (targetGroupResult.isLeft()) {
            return Either.left(targetGroupResult.getLeft());
        }
        final var targetGroup = targetGroupResult.get();

        final var targetPersonageResult = randomGroupPersonage.randomMember(targetGroup.id());
        if (targetPersonageResult.isLeft()) {
            return Either.left(ValentineError.InternalError.INSTANCE);
        }
        if (targetPersonageResult.get().isEmpty()) {
            return Either.left(ValentineError.TargetGroupIsEmpty.INSTANCE);
        }
        final var receiverId = targetPersonageResult.get().orElseThrow();

        if (senderId.equals(receiverId)) {
            return Either.left(ValentineError.CannotSendToSelf.INSTANCE);
        }

        final var receiver = personageService.getByIdForce(receiverId);

        final var goldCost = config.randomGroupGold();
        final var energyCost = config.randomGroupEnergyCost();

        return chargeAndSend(sender, receiver, goldCost, energyCost, true, senderGroupId, targetGroup.id())
            .map(pair -> new ValentineResult.RandomInGroup(
                pair.sender(),
                pair.receiver(),
                senderGroup,
                targetGroup,
                goldCost,
                energyCost,
                pair.senderBadge(),
                pair.receiverBadge()
            ));
    }

    private Either<ValentineError, Group> getAndValidateTargetGroup(Group senderGroup, String targetTag) {
        if (senderGroup.isSameTag(targetTag)) {
            return Either.left(ValentineError.SendToThisGroup.INSTANCE);
        }

        final var targetGroupOpt = getGroup.getByTag(targetTag);
        if (targetGroupOpt.isEmpty()) {
            return Either.left(ValentineError.TargetGroupNotFound.INSTANCE);
        }
        final var targetGroup = targetGroupOpt.get();
        if (!targetGroup.isActive()) {
            return Either.left(ValentineError.TargetGroupNotActive.INSTANCE);
        }

        return Either.right(targetGroup);
    }

    private Either<ValentineError, SendResult> chargeAndSend(
        Personage sender,
        Personage receiver,
        Money goldCost,
        int energyCost,
        boolean isRandom,
        GroupId throwingGroupId,
        GroupId targetGroupId
    ) {
        if (sender.money().lessThan(goldCost)) {
            return Either.left(new ValentineError.NotEnoughMoney(goldCost));
        }
        final var energyResult = personageService.reduceEnergy(sender, energyCost, TimeUtils.moscowTime());
        if (energyResult.isLeft()) {
            return Either.left(new ValentineError.NotEnoughEnergy(energyCost));
        }
        var updatedSender = energyResult.get();
        updatedSender = personageService.takeMoney(updatedSender, goldCost);

        storage.save(sender.id(), receiver.id(), isRandom, throwingGroupId, targetGroupId, TimeUtils.moscowTime());

        final boolean senderBadge = checkAndAwardBadge(sender.id());
        final boolean receiverBadge = checkAndAwardBadge(receiver.id());

        return Either.right(new SendResult(updatedSender, receiver, senderBadge, receiverBadge));
    }

    private boolean checkAndAwardBadge(PersonageId personageId) {
        final var counts = storage.getCounts(personageId);
        final var threshold = config.badgeThreshold();
        if (counts.sent() >= threshold && counts.received() >= threshold) {
            final var badges = personageBadgeStorage.getPersonageAvailableBadges(personageId);
            final boolean alreadyHas = badges.stream()
                .anyMatch(b -> b.badge().view() == BadgeView.VALENTINE);
            if (!alreadyHas) {
                return badgeService.getByCode(BadgeView.VALENTINE.code())
                    .map(badge -> {
                        personageBadgeStorage.savePersonageAvailableBadge(personageId, badge.id(), false);
                        return true;
                    })
                    .orElse(false);
            }
        }
        return false;
    }

    private record SendResult(
        Personage sender,
        Personage receiver,
        boolean senderBadge,
        boolean receiverBadge
    ) {
    }
}
