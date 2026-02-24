package ru.homyakin.seeker.game.group.action;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.error.DonateMoneyToGroupError;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.stats.action.GroupPersonageStatsService;
import ru.homyakin.seeker.test_utils.TestRandom;

import java.util.Optional;

class DonateToGroupCommandTest {
    private final GroupStorage groupStorage = Mockito.mock();
    private final PersonageService personageService = Mockito.mock();
    private final GroupPersonageStatsService groupPersonageStatsService = Mockito.mock();
    private final DonateToGroupCommand donateToGroupCommand = new DonateToGroupCommand(
        groupStorage, 
        personageService, 
        groupPersonageStatsService
    );
    private GroupId groupId;
    private PersonageId personageId;
    private Personage personage;

    @BeforeEach
    void init() {
        groupId = new GroupId(TestRandom.nextLong());
        personageId = new PersonageId(TestRandom.nextLong());
        personage = new Personage(
            personageId,
            "Test Personage",
            Optional.empty(),
            Optional.empty(),
            new Money(100),
            null,
            null,
            null,
            null,
            null
        );
    }

    @Test
    void When_DonationAmountIsNegative_Then_ReturnInvalidAmountError() {
        final var donationAmount = new Money(-50);

        final var result = donateToGroupCommand.execute(groupId, personageId, donationAmount);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(DonateMoneyToGroupError.InvalidAmount.INSTANCE, result.getLeft());
    }

    @Test
    void When_DonationAmountIsZero_Then_ReturnInvalidAmountError() {
        final var donationAmount = new Money(0);

        final var result = donateToGroupCommand.execute(groupId, personageId, donationAmount);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(DonateMoneyToGroupError.InvalidAmount.INSTANCE, result.getLeft());
    }

    @Test
    void When_DonationIsSuccessful_Then_UpdateBalances() {
        final var donationAmount = new Money(50);
        Mockito.when(personageService.getByIdForce(personageId)).thenReturn(personage);
        Mockito.when(personageService.takeMoney(personage, donationAmount)).thenReturn(
            new Personage(
                personageId,
                "Test Personage",
                Optional.empty(),
                Optional.empty(),
                new Money(50),
                null,
                null,
                null,
                null,
                null
            )
        );

        final var result = donateToGroupCommand.execute(groupId, personageId, donationAmount);

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(new Money(50), result.get().money());
        Mockito.verify(groupStorage).addMoney(groupId, donationAmount);
        Mockito.verify(groupPersonageStatsService).addDonateMoney(groupId, personageId, donationAmount);
    }

    @Test
    void When_DonationAmountExceedsBalance_Then_ReturnNotEnoughMoneyError() {
        final var donationAmount = new Money(150);
        Mockito.when(personageService.getByIdForce(personageId)).thenReturn(personage);

        final var result = donateToGroupCommand.execute(groupId, personageId, donationAmount);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(DonateMoneyToGroupError.NotEnoughMoney.INSTANCE, result.getLeft());
    }
}
