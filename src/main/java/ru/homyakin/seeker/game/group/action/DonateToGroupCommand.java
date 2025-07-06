package ru.homyakin.seeker.game.group.action;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.error.DonateMoneyToGroupError;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.stats.action.GroupPersonageStatsService;

@Component
public class DonateToGroupCommand {
    private final GroupStorage groupStorage;
    private final PersonageService personageService;
    private final GroupPersonageStatsService groupPersonageStatsService;

    public DonateToGroupCommand(
        GroupStorage groupStorage, 
        PersonageService personageService,
        GroupPersonageStatsService groupPersonageStatsService
    ) {
        this.groupStorage = groupStorage;
        this.personageService = personageService;
        this.groupPersonageStatsService = groupPersonageStatsService;
    }

    @Transactional
    public Either<DonateMoneyToGroupError, Personage> execute(
        GroupId groupId,
        PersonageId personageId,
        Money money
    ) {
        if (money.isNegative() || money.isZero()) {
            return Either.left(DonateMoneyToGroupError.InvalidAmount.INSTANCE);
        }
        final var personage = personageService.getByIdForce(personageId);
        if (personage.money().lessThan(money)) {
            return Either.left(DonateMoneyToGroupError.NotEnoughMoney.INSTANCE);
        }
        final var updatePersonage = personageService.takeMoney(personage, money);
        groupStorage.addMoney(groupId, money);
        groupPersonageStatsService.addDonateMoney(groupId, personageId, money);
        return Either.right(updatePersonage);
    }
}
