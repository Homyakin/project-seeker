package ru.homyakin.seeker.game.duel;

import io.vavr.control.Either;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.battle.Battle;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.duel.models.CreateDuelError;
import ru.homyakin.seeker.game.duel.models.CreateDuelResult;
import ru.homyakin.seeker.game.duel.models.Duel;
import ru.homyakin.seeker.game.duel.models.DuelPersonageResult;
import ru.homyakin.seeker.game.duel.models.DuelResult;
import ru.homyakin.seeker.game.duel.models.DuelStatus;
import ru.homyakin.seeker.game.duel.models.ProcessDuelError;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.utils.models.Success;

@Component
public class DuelService {
    private final DuelDao duelDao;
    private final Duration duelLifeTime;
    private final PersonageService personageService;
    private final ItemService itemService;
    private final LockService lockService;
    private final Battle battle = new Battle();

    public DuelService(
        DuelDao duelDao,
        DuelConfig duelConfig,
        PersonageService personageService,
        ItemService itemService,
        LockService lockService
    ) {
        this.duelDao = duelDao;
        this.duelLifeTime = duelConfig.lifeTime();
        this.personageService = personageService;
        this.itemService = itemService;
        this.lockService = lockService;
    }

    //TODO прочитать про transactional
    public Either<CreateDuelError, CreateDuelResult> createDuel(
        Personage initiatingPersonage,
        Personage acceptingPersonage,
        GroupId groupId
    ) {
        if (duelDao.getWaitingDuelByInitiatingPersonage(initiatingPersonage.id()).isPresent()) {
            return Either.left(new CreateDuelError.PersonageAlreadyHasDuel());
        }
        if (initiatingPersonage.money().lessThan(DUEL_PRICE)) {
            return Either.left(new CreateDuelError.InitiatingPersonageNotEnoughMoney(DUEL_PRICE));
        }

        personageService.takeMoney(initiatingPersonage, DUEL_PRICE);

        final var id = duelDao.create(initiatingPersonage.id(), acceptingPersonage.id(), groupId, duelLifeTime);
        return Either.right(
            new CreateDuelResult(id, initiatingPersonage, acceptingPersonage, DUEL_PRICE)
        );
    }

    public Duel getByIdForce(long duelId) {
        return duelDao.getById(duelId)
            .orElseThrow(() -> new IllegalStateException("Duel " + duelId + "must exist"));
    }

    public Either<ProcessDuelError, Success> expireDuel(long duelId) {
        return lockService.<Either<ProcessDuelError, Success>>tryLockAndCalc(
            duelLockKey(duelId),
            () -> {
                if (getByIdForce(duelId).isFinalStatus()) {
                    return Either.left(ProcessDuelError.DuelIsFinished.INSTANCE);
                }
                returnMoneyToInitiator(duelId);
                duelDao.updateStatus(duelId, DuelStatus.EXPIRED);
                return Either.right(Success.INSTANCE);
            }
        ).fold(
            _ -> Either.left(ProcessDuelError.DuelLocked.INSTANCE),
            either -> either
        );
    }

    public Either<ProcessDuelError, Success> declineDuel(Duel duel, PersonageId acceptor) {
        if (!acceptor.equals(duel.acceptingPersonageId())) {
            return Either.left(ProcessDuelError.NotDuelAcceptor.INSTANCE);
        }
        return lockService.<Either<ProcessDuelError, Success>>tryLockAndCalc(
            duelLockKey(duel.id()),
            () -> {
                if (duel.isFinalStatus()) {
                    return Either.left(ProcessDuelError.DuelIsFinished.INSTANCE);
                }
                returnMoneyToInitiator(duel.id());
                duelDao.updateStatus(duel.id(), DuelStatus.DECLINED);
                return Either.right(Success.INSTANCE);
            }
        ).fold(
            _ -> Either.left(ProcessDuelError.DuelLocked.INSTANCE),
            either -> either
        );
    }

    public Either<ProcessDuelError, DuelResult> finishDuel(Duel duel, PersonageId acceptor) {
        if (!acceptor.equals(duel.acceptingPersonageId())) {
            return Either.left(ProcessDuelError.NotDuelAcceptor.INSTANCE);
        }
        return lockService.tryLockAndCalc(
            duelLockKey(duel.id()),
            () -> finishDuelLogic(duel)
        ).fold(
            _ -> Either.left(ProcessDuelError.DuelLocked.INSTANCE),
            either -> either
        );
    }

    private Either<ProcessDuelError, DuelResult> finishDuelLogic(Duel duel) {
        if (duel.isFinalStatus()) {
            return Either.left(ProcessDuelError.DuelIsFinished.INSTANCE);
        }
        duelDao.updateStatus(duel.id(), DuelStatus.FINISHED);
        final var personage1 = personageService.getByIdForce(duel.initiatingPersonageId());
        final var personage2 = personageService.getByIdForce(duel.acceptingPersonageId());
        final var equippedItems = itemService.getEquippedItemsByPersonageIds(
            Set.of(personage1.id(), personage2.id())
        );
        final var firstBattlePersonage = BattlePersonage.forCombat(
            equippedItems.getOrDefault(personage1.id(), List.of()),
            Position.FRONT,
            personage1.effects()
        );
        final var secondBattlePersonage = BattlePersonage.forCombat(
            equippedItems.getOrDefault(personage2.id(), List.of()),
            Position.FRONT,
            personage2.effects()
        );
        final var battleResult = battle.process(
            List.of(firstBattlePersonage),
            List.of(secondBattlePersonage)
        );

        final DuelPersonageResult winner;
        final DuelPersonageResult loser;
        if (battleResult.firstWin()) {
            winner = new DuelPersonageResult(
                personage1,
                battleResult.personageStats().get(firstBattlePersonage.id())
            );
            loser = new DuelPersonageResult(
                personage2,
                battleResult.personageStats().get(secondBattlePersonage.id())
            );
        } else {
            winner = new DuelPersonageResult(
                personage2,
                battleResult.personageStats().get(secondBattlePersonage.id())
            );
            loser = new DuelPersonageResult(
                personage1,
                battleResult.personageStats().get(firstBattlePersonage.id())
            );
        }
        duelDao.addWinnerIdToDuel(duel.id(), winner.personage().id());
        return Either.right(new DuelResult(winner, loser));
    }

    private void returnMoneyToInitiator(long duelId) {
        final var initiatingPersonage = personageService.getByIdForce(getByIdForce(duelId).initiatingPersonageId());
        personageService.addMoney(initiatingPersonage, DUEL_PRICE);
    }

    private String duelLockKey(long duelId) {
        return LockPrefixes.DUEL.name() + "-" + duelId;
    }

    private static final Money DUEL_PRICE = new Money(2);
}
