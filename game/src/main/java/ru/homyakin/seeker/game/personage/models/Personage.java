package ru.homyakin.seeker.game.personage.models;

import io.vavr.control.Either;
import java.time.LocalDateTime;
import java.util.Optional;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.group.entity.Group;
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Personage other) {
            return this.id.equals(other.id);
        }
        return false;
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

    public static final Money CHANGE_NAME_COST = new Money(20);

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
