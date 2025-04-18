package ru.homyakin.seeker.game.group.action;


import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupProfile;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.TakeMoneyFromGroupError;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.test_utils.PersonageMemberGroupUtils;
import ru.homyakin.seeker.test_utils.TestRandom;

import java.util.Optional;

class TakeMoneyFromGroupCommandTest {

    private final GroupStorage groupStorage = Mockito.mock();
    private final GroupPersonageStorage groupPersonageStorage = Mockito.mock();
    private final PersonageService personageService = Mockito.mock();
    private final TakeMoneyFromGroupCommand takeMoneyFromGroupCommand = new TakeMoneyFromGroupCommand(
        groupStorage,
        groupPersonageStorage,
        personageService
    );
    private GroupId groupId;
    private PersonageId personageId;

    @BeforeEach
    void init() {
        groupId = new GroupId(TestRandom.nextLong());
        personageId = new PersonageId(TestRandom.nextLong());
    }

    @Test
    void When_WithdrawNegativeAmount_Then_ReturnInvalidAmountError() {
        Money negativeAmount = Money.from(-100);

        final var result = takeMoneyFromGroupCommand.execute(groupId, personageId, negativeAmount);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(TakeMoneyFromGroupError.InvalidAmount.INSTANCE, result.getLeft());
    }

    @Test
    void When_WithdrawZeroAmount_Then_ReturnInvalidAmountError() {
        final var zeroAmount = Money.from(0);

        final var result = takeMoneyFromGroupCommand.execute(groupId, personageId, zeroAmount);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(TakeMoneyFromGroupError.InvalidAmount.INSTANCE, result.getLeft());
    }

    @Test
    void When_WithdrawMoreThanAvailableFunds_Then_ReturnNotEnoughMoneyError() {
        final var withdrawalAmount = Money.from(2000);
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(groupProfile()));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));

        final var result = takeMoneyFromGroupCommand.execute(groupId, personageId, withdrawalAmount);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(TakeMoneyFromGroupError.NotEnoughMoney.INSTANCE, result.getLeft());
    }

    @Test
    void When_WithdrawFromNonMemberPersonage_Then_ReturnPersonageNotMemberError() {
        final var validAmount = new Money(100);
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(groupProfile()));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.empty());

        final var result = takeMoneyFromGroupCommand.execute(groupId, personageId, validAmount);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(TakeMoneyFromGroupError.PersonageNotMember.INSTANCE, result.getLeft());
    }

    @Test
    void When_WithdrawFromNotRegisteredGroup_Then_ReturnGroupNotRegisteredError() {
        final var validAmount = new Money(100);
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(groupProfile(Optional.empty())));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));

        final var result = takeMoneyFromGroupCommand.execute(groupId, personageId, validAmount);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(TakeMoneyFromGroupError.GroupNotRegistered.INSTANCE, result.getLeft());
    }

    @Test
    void When_SuccessfullyWithdrawValidAmount_Then_UpdateBalances() {
        Money validAmount = new Money(500);
        Personage personage = new Personage(personageId, "Test Personage", Optional.empty(), new Money(0), null, null, null, null, null);
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(groupProfile()));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));
        Mockito.when(personageService.getByIdForce(personageId)).thenReturn(personage);

        final var result = takeMoneyFromGroupCommand.execute(groupId, personageId, validAmount);

        Assertions.assertTrue(result.isRight());
        Mockito.verify(groupStorage).takeMoney(groupId, validAmount);
        Mockito.verify(personageService).addMoney(personage, validAmount);
    }

    private GroupProfile groupProfile() {
        return groupProfile(Optional.of("tag"));
    }

    private GroupProfile groupProfile(Optional<String> tag) {
        return new GroupProfile(
            groupId,
            "Test Group",
            tag,
            new Money(1000),
            1
        );
    }

}
