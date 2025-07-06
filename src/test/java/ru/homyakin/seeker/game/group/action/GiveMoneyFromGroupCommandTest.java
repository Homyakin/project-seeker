package ru.homyakin.seeker.game.group.action;


import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.action.personage.CheckGroupMemberAdminCommand;
import ru.homyakin.seeker.game.group.entity.GroupProfile;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.CheckGroupMemberAdminError;
import ru.homyakin.seeker.game.group.error.GiveMoneyFromGroupError;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.stats.action.GroupPersonageStatsService;
import ru.homyakin.seeker.test_utils.PersonageMemberGroupUtils;
import ru.homyakin.seeker.test_utils.TestRandom;
import ru.homyakin.seeker.utils.models.Success;

import java.util.Optional;

class GiveMoneyFromGroupCommandTest {

    private final GroupStorage groupStorage = Mockito.mock();
    private final GroupPersonageStorage groupPersonageStorage = Mockito.mock();
    private final PersonageService personageService = Mockito.mock();
    private final CheckGroupMemberAdminCommand checkGroupMemberAdminCommand = Mockito.mock();
    private final GroupPersonageStatsService groupPersonageStatsService = Mockito.mock();
    private final GiveMoneyFromGroupCommand takeMoneyFromGroupCommand = new GiveMoneyFromGroupCommand(
        groupStorage,
        checkGroupMemberAdminCommand,
        groupPersonageStorage,
        personageService,
        groupPersonageStatsService
    );
    private GroupId groupId;
    private PersonageId acceptor;
    private PersonageId giver;

    @BeforeEach
    void init() {
        groupId = new GroupId(TestRandom.nextLong());
        acceptor = new PersonageId(TestRandom.nextLong());
        giver = new PersonageId(TestRandom.nextLong());
        Mockito.when(checkGroupMemberAdminCommand.execute(groupId, giver))
            .thenReturn(Either.right(Success.INSTANCE));
    }

    @Test
    void When_WithdrawNegativeAmount_Then_ReturnInvalidAmountError() {
        Money negativeAmount = Money.from(-100);

        final var result = takeMoneyFromGroupCommand.execute(groupId, giver, acceptor, negativeAmount);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(GiveMoneyFromGroupError.InvalidAmount.INSTANCE, result.getLeft());
    }

    @Test
    void When_WithdrawZeroAmount_Then_ReturnInvalidAmountError() {
        final var zeroAmount = Money.from(0);

        final var result = takeMoneyFromGroupCommand.execute(groupId, giver, acceptor, zeroAmount);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(GiveMoneyFromGroupError.InvalidAmount.INSTANCE, result.getLeft());
    }

    @Test
    void When_WithdrawMoreThanAvailableFunds_Then_ReturnNotEnoughMoneyError() {
        final var withdrawalAmount = Money.from(2000);
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(groupProfile()));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(acceptor))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));

        final var result = takeMoneyFromGroupCommand.execute(groupId, giver, acceptor, withdrawalAmount);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(GiveMoneyFromGroupError.NotEnoughMoney.INSTANCE, result.getLeft());
    }

    @Test
    void When_WithdrawFromNonMemberPersonage_Then_ReturnPersonageNotMemberError() {
        final var validAmount = new Money(100);
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(groupProfile()));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(acceptor))
            .thenReturn(PersonageMemberGroupUtils.empty());

        final var result = takeMoneyFromGroupCommand.execute(groupId, giver, acceptor, validAmount);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(GiveMoneyFromGroupError.PersonageNotMember.INSTANCE, result.getLeft());
    }

    @Test
    void When_WithdrawFromNotRegisteredGroup_Then_ReturnGroupNotRegisteredError() {
        final var validAmount = new Money(100);
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(groupProfile(Optional.empty())));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(acceptor))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));

        final var result = takeMoneyFromGroupCommand.execute(groupId, giver, acceptor, validAmount);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(GiveMoneyFromGroupError.GroupNotRegistered.INSTANCE, result.getLeft());
    }

    @Test
    void When_WithdrawGiverNotMember_Then_ReturnNotMemberError() {
        final var validAmount = new Money(100);
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(groupProfile()));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(acceptor))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));
        Mockito.when(checkGroupMemberAdminCommand.execute(groupId, giver))
            .thenReturn(Either.left(CheckGroupMemberAdminError.PersonageNotInGroup.INSTANCE));

        final var result = takeMoneyFromGroupCommand.execute(groupId, giver, acceptor, validAmount);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(CheckGroupMemberAdminError.PersonageNotInGroup.INSTANCE, result.getLeft());
    }

    @Test
    void When_WithdrawGiverNotAdmin_Then_ReturnNotAdminError() {
        final var validAmount = new Money(100);
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(groupProfile()));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(acceptor))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));
        Mockito.when(checkGroupMemberAdminCommand.execute(groupId, giver))
            .thenReturn(Either.left(CheckGroupMemberAdminError.NotAnAdmin.INSTANCE));

        final var result = takeMoneyFromGroupCommand.execute(groupId, giver, acceptor, validAmount);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(CheckGroupMemberAdminError.NotAnAdmin.INSTANCE, result.getLeft());
    }

    @Test
    void When_SuccessfullyWithdrawValidAmount_Then_UpdateBalances() {
        Money validAmount = new Money(500);
        Personage personage = new Personage(acceptor, "Test Personage", Optional.empty(), new Money(0), null, null, null, null, null);
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(groupProfile()));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(acceptor))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));
        Mockito.when(personageService.getByIdForce(acceptor)).thenReturn(personage);

        final var result = takeMoneyFromGroupCommand.execute(groupId, giver, acceptor, validAmount);

        Assertions.assertTrue(result.isRight());
        Mockito.verify(groupStorage).takeMoney(groupId, validAmount);
        Mockito.verify(personageService).addMoney(personage, validAmount);
        Mockito.verify(groupPersonageStatsService).addGiveMoney(groupId, acceptor, validAmount);
    }

    private GroupProfile groupProfile() {
        return groupProfile(Optional.of("tag"));
    }

    private GroupProfile groupProfile(Optional<String> tag) {
        return new GroupProfile(
            groupId,
            "Test Group",
            tag,
            null,
            new Money(1000),
            1
        );
    }

}
