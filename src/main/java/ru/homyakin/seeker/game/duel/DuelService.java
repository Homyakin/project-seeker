package ru.homyakin.seeker.game.duel;

import io.vavr.control.Either;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.duel.models.CreateDuelError;
import ru.homyakin.seeker.game.duel.models.Duel;
import ru.homyakin.seeker.game.duel.models.DuelStatus;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class DuelService {
    private final DuelDao duelDao;
    private final Duration duelLifeTime;

    public DuelService(DuelDao duelDao, DuelConfig duelConfig) {
        this.duelDao = duelDao;
        this.duelLifeTime = duelConfig.lifeTime();
    }

    public Either<CreateDuelError, Duel> createDuel(
        Personage initiatingPersonage,
        Personage acceptingPersonage,
        long groupId
    ) {
        if (duelDao.getWaitingDuelByInitiatingPersonage(initiatingPersonage.id()).isPresent()) {
            return Either.left(new CreateDuelError.PersonageAlreadyHasDuel());
        }
        if (initiatingPersonage.hasHealthLessThanPercent(PERSONAGE_MINIMUM_HP_PERCENT)) {
            return Either.left(new CreateDuelError.InitiatingPersonageHasLowHealth());
        }
        if (acceptingPersonage.hasHealthLessThanPercent(PERSONAGE_MINIMUM_HP_PERCENT)) {
            return Either.left(new CreateDuelError.AcceptingPersonageHasLowHealth());
        }

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
        duelDao.updateStatus(duelId, DuelStatus.EXPIRED);
    }

    public void declineDuel(long duelId) {
        //TODO проверка на то, что статус был не финишд
        duelDao.updateStatus(duelId, DuelStatus.DECLINED);
    }

    public void finishDuel(long duelId) {
        //TODO проверка на то, что статус был не финишд
        duelDao.updateStatus(duelId, DuelStatus.FINISHED);
    }

    public void addWinner(long duelId, long personageId) {
        //TODO проверка на то, что статус был не финишд
        duelDao.addWinnerIdToDuel(duelId, personageId);
    }

    private static final double PERSONAGE_MINIMUM_HP_PERCENT = 0.1;
}
