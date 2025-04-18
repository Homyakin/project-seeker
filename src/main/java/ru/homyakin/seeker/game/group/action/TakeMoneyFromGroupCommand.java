package ru.homyakin.seeker.game.group.action;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.TakeMoneyFromGroupError;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;

@Component
public class TakeMoneyFromGroupCommand {
    private final GroupStorage groupStorage;
    private final GroupPersonageStorage groupPersonageStorage;
    private final PersonageService personageService;

    public TakeMoneyFromGroupCommand(
        GroupStorage groupStorage,
        GroupPersonageStorage groupPersonageStorage,
        PersonageService personageService
    ) {
        this.groupStorage = groupStorage;
        this.groupPersonageStorage = groupPersonageStorage;
        this.personageService = personageService;
    }

    @Transactional
    public Either<TakeMoneyFromGroupError, Personage> execute(
        GroupId groupId,
        PersonageId personageId,
        Money money
    ) {
        if (money.isNegative() || money.isZero()) {
            return Either.left(TakeMoneyFromGroupError.InvalidAmount.INSTANCE);
        }
        final var group = groupStorage.getProfile(groupId).orElseThrow();
        if (!group.isRegistered()) {
            return Either.left(TakeMoneyFromGroupError.GroupNotRegistered.INSTANCE);
        }
        if (group.money().lessThan(money)) {
            return Either.left(TakeMoneyFromGroupError.NotEnoughMoney.INSTANCE);
        }
        final var personageMemberGroup = groupPersonageStorage.getPersonageMemberGroup(personageId);
        if (!personageMemberGroup.isGroupMember(groupId)) {
            return Either.left(TakeMoneyFromGroupError.PersonageNotMember.INSTANCE);
        }
        final var personage = personageService.getByIdForce(personageId);
        final var updatePersonage = personageService.addMoney(personage, money);
        groupStorage.takeMoney(groupId, money);
        return Either.right(updatePersonage);
    }
}
