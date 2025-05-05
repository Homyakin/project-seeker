package ru.homyakin.seeker.game.group.action;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.action.personage.CheckGroupMemberAdminCommand;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.GiveMoneyFromGroupError;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;

@Component
public class GiveMoneyFromGroupCommand {
    private final GroupStorage groupStorage;
    private final CheckGroupMemberAdminCommand checkGroupMemberAdminCommand;
    private final GroupPersonageStorage groupPersonageStorage;
    private final PersonageService personageService;

    public GiveMoneyFromGroupCommand(
        GroupStorage groupStorage,
        CheckGroupMemberAdminCommand checkGroupMemberAdminCommand,
        GroupPersonageStorage groupPersonageStorage,
        PersonageService personageService
    ) {
        this.groupStorage = groupStorage;
        this.checkGroupMemberAdminCommand = checkGroupMemberAdminCommand;
        this.groupPersonageStorage = groupPersonageStorage;
        this.personageService = personageService;
    }

    @Transactional
    public Either<GiveMoneyFromGroupError, Personage> execute(
        GroupId groupId,
        PersonageId givingPersonageId,
        PersonageId receivingPersonageId,
        Money money
    ) {
        final var adminCheckResult = checkGroupMemberAdminCommand.execute(groupId, givingPersonageId);
        if (adminCheckResult.isLeft()) {
            return Either.left(adminCheckResult.getLeft());
        }
        if (money.isNegative() || money.isZero()) {
            return Either.left(GiveMoneyFromGroupError.InvalidAmount.INSTANCE);
        }
        final var group = groupStorage.getProfile(groupId).orElseThrow();
        if (!group.isRegistered()) {
            return Either.left(GiveMoneyFromGroupError.GroupNotRegistered.INSTANCE);
        }
        if (group.money().lessThan(money)) {
            return Either.left(GiveMoneyFromGroupError.NotEnoughMoney.INSTANCE);
        }
        if (!groupPersonageStorage.getPersonageMemberGroup(receivingPersonageId).isGroupMember(groupId)) {
            return Either.left(GiveMoneyFromGroupError.PersonageNotMember.INSTANCE);
        }

        final var personage = personageService.getByIdForce(receivingPersonageId);
        final var updatePersonage = personageService.addMoney(personage, money);
        groupStorage.takeMoney(groupId, money);
        return Either.right(updatePersonage);
    }
}
