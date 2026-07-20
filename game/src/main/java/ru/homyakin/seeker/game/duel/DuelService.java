package ru.homyakin.seeker.game.duel;

import io.vavr.control.Either;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.battle.Battle;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.EventBattleLogService;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.duel.models.CreateDuelError;
import ru.homyakin.seeker.game.duel.models.CreateDuelResult;
import ru.homyakin.seeker.game.duel.models.Duel;
import ru.homyakin.seeker.game.duel.models.DuelPersonageResult;
import ru.homyakin.seeker.game.duel.models.DuelResult;
import ru.homyakin.seeker.game.duel.models.ProcessDuelError;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.event.service.EventService;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.event.AddPersonageToEventRequest;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.utils.TimeUtils;
import ru.homyakin.seeker.utils.models.Success;

@Component
public class DuelService {
    private final DuelDao duelDao;
    private final DuelConfig duelConfig;
    private final PersonageService personageService;
    private final EquipmentLoadoutService loadoutService;
    private final LockService lockService;
    private final LaunchedEventService launchedEventService;
    private final EventService eventService;
    private final PersonageEventService personageEventService;
    private final EventBattleLogService eventBattleLogService;
    private final Battle battle = new Battle();

    public DuelService(
        DuelDao duelDao,
        DuelConfig duelConfig,
        PersonageService personageService,
        EquipmentLoadoutService loadoutService,
        LockService lockService,
        LaunchedEventService launchedEventService,
        EventService eventService,
        PersonageEventService personageEventService,
        EventBattleLogService eventBattleLogService
    ) {
        this.duelDao = duelDao;
        this.duelConfig = duelConfig;
        this.personageService = personageService;
        this.loadoutService = loadoutService;
        this.lockService = lockService;
        this.launchedEventService = launchedEventService;
        this.eventService = eventService;
        this.personageEventService = personageEventService;
        this.eventBattleLogService = eventBattleLogService;
    }

    @Transactional
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

        final var duelEvent = eventService.getByType(EventType.DUEL)
            .orElseThrow(() -> new IllegalStateException("Duel event template must exist"));

        personageService.takeMoney(initiatingPersonage, DUEL_PRICE);

        final var now = TimeUtils.moscowTime();
        final var launchedEvent = launchedEventService.createFromDuel(
            duelEvent.id(),
            now,
            now.plus(duelConfig.lifeTime()),
            groupId
        );

        duelDao.create(launchedEvent.id(), initiatingPersonage.id(), acceptingPersonage.id());
        personageEventService.addPersonageToLaunchedEvent(new AddPersonageToEventRequest(
            launchedEvent.id(), initiatingPersonage.id(), Optional.empty(), 0
        )).getOrElseThrow(_ -> new IllegalStateException("Failed to add initiator to duel event"));
        personageEventService.addPersonageToLaunchedEvent(new AddPersonageToEventRequest(
            launchedEvent.id(), acceptingPersonage.id(), Optional.empty(), 0
        )).getOrElseThrow(_ -> new IllegalStateException("Failed to add acceptor to duel event"));

        return Either.right(
            new CreateDuelResult(launchedEvent.id(), initiatingPersonage, acceptingPersonage, DUEL_PRICE)
        );
    }

    public Duel getByIdForce(long duelId) {
        return duelDao.getById(duelId)
            .orElseThrow(() -> new IllegalStateException("Duel " + duelId + " must exist"));
    }

    public Either<ProcessDuelError, Success> expireDuel(long duelId) {
        return lockService.<Either<ProcessDuelError, Success>>tryLockAndCalc(
            launchedEventLockKey(duelId),
            () -> {
                if (getByIdForce(duelId).isFinalStatus()) {
                    return Either.left(ProcessDuelError.DuelIsFinished.INSTANCE);
                }
                returnMoneyToInitiator(duelId);
                launchedEventService.updateStatus(duelId, EventStatus.EXPIRED);
                return Either.right(Success.INSTANCE);
            }
        ).fold(
            _ -> Either.left(ProcessDuelError.DuelLocked.INSTANCE),
            either -> either
        );
    }

    public EventResult.DuelResult expireLaunchedDuel(LaunchedEvent launchedEvent) {
        final var duel = getByIdForce(launchedEvent.id());
        if (duel.isFinalStatus()) {
            return EventResult.DuelResult.AlreadyFinal.INSTANCE;
        }
        returnMoneyToInitiator(duel.id());
        launchedEventService.updateStatus(duel.id(), EventStatus.EXPIRED);
        return EventResult.DuelResult.Expired.INSTANCE;
    }

    public Either<ProcessDuelError, Success> declineDuel(Duel duel, PersonageId acceptor) {
        if (!acceptor.equals(duel.acceptingPersonageId())) {
            return Either.left(ProcessDuelError.NotDuelAcceptor.INSTANCE);
        }
        return lockService.<Either<ProcessDuelError, Success>>tryLockAndCalc(
            launchedEventLockKey(duel.id()),
            () -> {
                if (getByIdForce(duel.id()).isFinalStatus()) {
                    return Either.left(ProcessDuelError.DuelIsFinished.INSTANCE);
                }
                returnMoneyToInitiator(duel.id());
                launchedEventService.cancel(duel.id());
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
            launchedEventLockKey(duel.id()),
            () -> finishDuelLogic(duel.id())
        ).fold(
            _ -> Either.left(ProcessDuelError.DuelLocked.INSTANCE),
            either -> either
        );
    }

    private Either<ProcessDuelError, DuelResult> finishDuelLogic(long duelId) {
        final var duel = getByIdForce(duelId);
        if (duel.isFinalStatus()) {
            return Either.left(ProcessDuelError.DuelIsFinished.INSTANCE);
        }
        launchedEventService.updateStatus(duel.id(), EventStatus.SUCCESS);
        final var personage1 = personageService.getByIdForce(duel.initiatingPersonageId());
        final var personage2 = personageService.getByIdForce(duel.acceptingPersonageId());
        final var combatGear = loadoutService.resolveCombatGear(
            List.of(personage1, personage2),
            EventType.DUEL
        );
        final var firstGear = combatGear.get(personage1.id());
        final var secondGear = combatGear.get(personage2.id());
        final var firstBattlePersonage = BattlePersonage.forCombat(
            firstGear.items(),
            Position.FRONT,
            personage1.effects(),
            Optional.of(LocaleUtils.personageNameWithBadge(personage1))
        );
        final var secondBattlePersonage = BattlePersonage.forCombat(
            secondGear.items(),
            Position.FRONT,
            personage2.effects(),
            Optional.of(LocaleUtils.personageNameWithBadge(personage2))
        );
        final var battleResult = battle.process(
            List.of(firstBattlePersonage),
            List.of(secondBattlePersonage)
        );
        eventBattleLogService.save(duel.id(), battleResult);

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
        return Either.right(new DuelResult(winner, loser, battleResult.rounds()));
    }

    private void returnMoneyToInitiator(long duelId) {
        final var initiatingPersonage = personageService.getByIdForce(getByIdForce(duelId).initiatingPersonageId());
        personageService.addMoney(initiatingPersonage, DUEL_PRICE);
    }

    private String launchedEventLockKey(long launchedEventId) {
        return LockPrefixes.LAUNCHED_EVENT.name() + launchedEventId;
    }

    public static final Money DUEL_PRICE = new Money(2);
}
