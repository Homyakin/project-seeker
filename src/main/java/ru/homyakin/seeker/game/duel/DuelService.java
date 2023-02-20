package ru.homyakin.seeker.game.duel;

import io.vavr.control.Either;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.duel.models.DuelError;
import ru.homyakin.seeker.game.duel.models.Duel;
import ru.homyakin.seeker.game.duel.models.DuelStatus;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.utils.TimeUtils;
import ru.homyakin.seeker.utils.models.Success;

@Component
public class DuelService {
    private final DuelDao duelDao;
    private final Duration duelLifeTime;
    private final PersonageService personageService;

    public DuelService(DuelDao duelDao, DuelConfig duelConfig, PersonageService personageService) {
        this.duelDao = duelDao;
        this.duelLifeTime = duelConfig.lifeTime();
        this.personageService = personageService;
    }

    //TODO прочитать про transactional
    public Either<DuelError, Duel> createDuel(
        Personage initiatingPersonage,
        Personage acceptingPersonage,
        long groupId
    ) {
        if (duelDao.getWaitingDuelByInitiatingPersonage(initiatingPersonage.id()).isPresent()) {
            return Either.left(new DuelError.PersonageAlreadyHasDuel());
        }
        if (initiatingPersonage.money().lessThan(DUEL_PRICE)) {
            return Either.left(new DuelError.InitiatingPersonageNotEnoughMoney(DUEL_PRICE));
        }
        if (acceptingPersonage.money().lessThan(DUEL_PRICE)) {
            return Either.left(new DuelError.AcceptingPersonageNotEnoughMoney(DUEL_PRICE));
        }

        personageService.takeMoney(initiatingPersonage, DUEL_PRICE);

        final var id = duelDao.create(initiatingPersonage.id(), acceptingPersonage.id(), groupId, duelLifeTime);
        return Either.right(getByIdForce(id));
    }

    public Duel getByIdForce(long duelId) {
        return duelDao.getById(duelId)
            .orElseThrow(() -> new IllegalStateException("Duel " + duelId + "must exist"));
    }

    public void addMessageIdToDuel(long duelId, int messageId) {
        duelDao.addMessageIdToDuel(duelId, messageId);
    }

    public List<Duel> getExpiringDuels() {
        return duelDao.getWaitingDuelsWithLessExpireDate(TimeUtils.moscowTime());
    }

    public void expireDuel(long duelId) {
        //TODO проверка на то, что статус был вейтинг
        returnMoneyToInitiator(duelId);
        duelDao.updateStatus(duelId, DuelStatus.EXPIRED);
    }

    public void declineDuel(long duelId) {
        //TODO проверка на то, что статус был не финишд
        returnMoneyToInitiator(duelId);
        duelDao.updateStatus(duelId, DuelStatus.DECLINED);
    }

    public Either<DuelError.AcceptingPersonageNotEnoughMoney, Success> finishDuel(long duelId) {
        //TODO проверка на то, что статус был не финишд
        final var acceptingPersonage = personageService.getByIdForce(getByIdForce(duelId).acceptingPersonageId());
        if (acceptingPersonage.money().lessThan(DUEL_PRICE)) {
            return Either.left(new DuelError.AcceptingPersonageNotEnoughMoney(DUEL_PRICE));
        }
        personageService.takeMoney(acceptingPersonage, DUEL_PRICE);
        duelDao.updateStatus(duelId, DuelStatus.FINISHED);
        return Either.right(new Success());
    }

    public void addWinner(long duelId, long personageId) {
        //TODO проверка на то, что статус был не финишд
        duelDao.addWinnerIdToDuel(duelId, personageId);
    }

    private void returnMoneyToInitiator(long duelId) {
        final var initiatingPersonage = personageService.getByIdForce(getByIdForce(duelId).initiatingPersonageId());
        personageService.addMoney(initiatingPersonage, DUEL_PRICE.value());
    }

    private static final Money DUEL_PRICE = new Money(3);
}
