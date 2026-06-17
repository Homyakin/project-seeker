package ru.homyakin.seeker.game.personage.models;

import io.vavr.control.Either;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.item.errors.PutOnItemError;
import ru.homyakin.seeker.game.item.errors.TakeOffItemError;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughEnergy;
import ru.homyakin.seeker.game.personage.models.errors.StillSame;
import ru.homyakin.seeker.game.utils.NameError;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughMoney;
import ru.homyakin.seeker.game.utils.NameValidator;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.utils.models.Success;

public record Personage(
    PersonageId id,
    String name,
    Optional<String> tag,
    Optional<GroupId> memberGroupId,
    Money money,
    Energy energy,
    BadgeView badge,
    PersonageEffects effects,
    Position position
) {
    public Personage addMoney(Money money) {
        return copyWithMoney(this.money.add(money));
    }

    public Either<NameError, Personage> changeName(String name) {
        return NameValidator.validateName(name).map(this::copyWithName);
    }

    public Either<StillSame, Personage> updateStateIfNeed(LocalDateTime now) {
        return updateStateIfNeed(now, true);
    }

    public Either<StillSame, Personage> updateStateIfNeed(LocalDateTime now, boolean regenEnergy) {
        final Either<StillSame, Energy> energyResult;
        if (regenEnergy) {
            energyResult = energy.regenIfNeeded(now);
        } else {
            energyResult = Either.left(StillSame.INSTANCE);
        }
        final var effectsResult = effects.expireIfNeeded(now);

        if (energyResult.isLeft() && effectsResult.isLeft()) {
            return Either.left(StillSame.INSTANCE);
        }

        final var personage = copyWithState(
            energyResult.getOrElse(energy),
            effectsResult.getOrElse(effects)
        );
        return Either.right(personage);
    }

    public Personage withPosition(Position position) {
        return copyWithPosition(position);
    }

    public Either<NotEnoughEnergy, Personage> reduceEnergy(
        LocalDateTime energyChangeTime,
        int energyToReduce
    ) {
        return energy.reduce(energyToReduce, energyChangeTime)
            .map(this::copyWithEnergy);
    }

    public Personage addEnergy(
        LocalDateTime energyChangeTime,
        int energyToAdd
    ) {
        return copyWithEnergy(energy.add(energyToAdd, energyChangeTime));
    }

    public Either<NotEnoughMoney, Personage> initChangeName() {
        if (money.lessThan(CHANGE_NAME_COST)) {
            return Either.left(new NotEnoughMoney(CHANGE_NAME_COST));
        }

        return Either.right(addMoney(CHANGE_NAME_COST.negative()));
    }

    public Either<NotEnoughMoney, Personage> cancelChangeName() {
        return Either.right(addMoney(CHANGE_NAME_COST));
    }

    public boolean hasEnoughEnergy(int requiredEnergy) {
        return energy.isGreaterOrEqual(requiredEnergy);
    }

    public int maxBagSize() {
        return MAX_BAG_SIZE;
    }

    public boolean hasSpaceInBagForItems(List<PersonageItem> items) {
        return items.stream().filter(it -> !it.isEquipped()).count() < maxBagSize();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Personage other) {
            return this.id.equals(other.id);
        }
        return false;
    }

    public Either<PutOnItemError, Success> canPutOnItem(List<PersonageItem> personageItems, PersonageItem item) {
        if (!item.personageId().map(it -> it.equals(id)).orElse(false)) {
            return Either.left(PutOnItemError.PersonageMissingItem.INSTANCE);
        }
        if (item.isEquipped()) {
            return Either.left(PutOnItemError.AlreadyEquipped.INSTANCE);
        }

        final var personageBusySlots = new HashMap<PersonageSlot, Integer>();
        for (final var slot : item.object().slots()) {
            personageBusySlots.computeIfPresent(slot, (k, v) -> v + 1);
            personageBusySlots.putIfAbsent(slot, 1);
        }
        for (final var personageItem : personageItems) {
            if (personageItem.isEquipped()) {
                for (final var slot : personageItem.object().slots()) {
                    personageBusySlots.computeIfPresent(slot, (k, v) -> v + 1);
                }
            }
        }
        final var missingSlots = new ArrayList<PersonageSlot>();
        for (final var entry : personageBusySlots.entrySet()) {
            if (entry.getValue() > personageAvailableSlots.getOrDefault(entry.getKey(), 0)) {
                missingSlots.add(entry.getKey());
            }
        }
        if (missingSlots.isEmpty()) {
            return Either.right(Success.INSTANCE);
        }
        return Either.left(new PutOnItemError.RequiredFreeSlots(missingSlots));
    }

    public Either<TakeOffItemError, Success> canTakeOffItem(List<PersonageItem> personageItems, PersonageItem item) {
        if (!item.personageId().map(it -> it.equals(id)).orElse(false)) {
            return Either.left(TakeOffItemError.PersonageMissingItem.INSTANCE);
        }
        if (!item.isEquipped()) {
            return Either.left(TakeOffItemError.AlreadyTakenOff.INSTANCE);
        }
        int itemsInBag = 0;
        for (final var personageItem : personageItems) {
            if (personageItem.isEquipped()) {
                ++itemsInBag;
            }
        }

        if (itemsInBag >= maxBagSize()) {
            return Either.left(TakeOffItemError.NotEnoughSpaceInBag.INSTANCE);
        }
        return Either.right(Success.INSTANCE);
    }

    public List<PersonageSlot> getFreeSlotsForItems(List<PersonageItem> personageItems) {
        return getFreeSlotsFromEquipped(personageItems.stream()
            .filter(PersonageItem::isEquipped)
            .map(item -> item.object().slots())
            .toList());
    }

    private List<PersonageSlot> getFreeSlotsFromEquipped(List<Set<PersonageSlot>> equippedItemSlots) {
        final var freeSlots = new HashMap<>(personageAvailableSlots);
        for (final var slots : equippedItemSlots) {
            for (final var slot : slots) {
                freeSlots.computeIfPresent(slot, (k, v) -> v - 1);
            }
        }
        final var result = new ArrayList<PersonageSlot>();
        for (final var entry : freeSlots.entrySet()) {
            for (int i = 0; i < entry.getValue(); ++i) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public Personage addEffect(PersonageEffectType type, PersonageEffect effect) {
        return copyWithEffects(effects.addEffect(type, effect));
    }

    public boolean isRegisteredGroupMember() {
        return tag().isPresent();
    }

    public boolean isSameGroup(Personage other) {
        return isRegisteredGroupMember() && tag().equals(other.tag());
    }

    public boolean isRegisteredGroupMember(Group group) {
        return isRegisteredGroupMember() && group.tag().isPresent() && tag().equals(group.tag());
    }

    private Personage copyWithEffects(PersonageEffects effects) {
        return new Personage(
            id,
            name,
            tag,
            memberGroupId,
            money,
            energy,
            badge,
            effects,
            position
        );
    }

    private static final int MAX_BAG_SIZE = 15;
    public static final Money CHANGE_NAME_COST = new Money(20);
    private static final Map<PersonageSlot, Integer> personageAvailableSlots = new HashMap<>() {{
        put(PersonageSlot.MAIN_HAND, 1);
        put(PersonageSlot.OFF_HAND, 1);
        put(PersonageSlot.BODY, 1);
        put(PersonageSlot.PANTS, 1);
        put(PersonageSlot.HELMET, 1);
        put(PersonageSlot.GLOVES, 1);
        put(PersonageSlot.SHOES, 1);
    }};

    private Personage copyWithName(String name) {
        return new Personage(
            id,
            name,
            tag,
            memberGroupId,
            money,
            energy,
            badge,
            effects,
            position
        );
    }

    private Personage copyWithEnergy(Energy energy) {
        return new Personage(
            id,
            name,
            tag,
            memberGroupId,
            money,
            energy,
            badge,
            effects,
            position
        );
    }

    private Personage copyWithMoney(Money money) {
        return new Personage(
            id,
            name,
            tag,
            memberGroupId,
            money,
            energy,
            badge,
            effects,
            position
        );
    }

    private Personage copyWithPosition(Position position) {
        return new Personage(
            id,
            name,
            tag,
            memberGroupId,
            money,
            energy,
            badge,
            effects,
            position
        );
    }

    private Personage copyWithState(Energy energy, PersonageEffects effects) {
        return new Personage(
            id,
            name,
            tag,
            memberGroupId,
            money,
            energy,
            badge,
            effects,
            position
        );
    }
}
