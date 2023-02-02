package ru.homyakin.seeker.telegram.command.group.duel;

import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.TwoPersonageTeamsBattle;
import ru.homyakin.seeker.game.duel.DuelService;
import ru.homyakin.seeker.game.duel.DuelStatus;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class AcceptDuelExecutor extends CommandExecutor<AcceptDuel> {
    private static final Logger logger = LoggerFactory.getLogger(AcceptDuelExecutor.class);
    private final GroupUserService groupUserService;
    private final DuelService duelService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;
    private final TwoPersonageTeamsBattle twoPersonageTeamsBattle;

    public AcceptDuelExecutor(
        GroupUserService groupUserService,
        DuelService duelService,
        PersonageService personageService,
        TelegramSender telegramSender,
        TwoPersonageTeamsBattle twoPersonageTeamsBattle
    ) {
        this.groupUserService = groupUserService;
        this.duelService = duelService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
        this.twoPersonageTeamsBattle = twoPersonageTeamsBattle;
    }

    @Override
    public void execute(AcceptDuel command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId());
        final var duel = duelService.getByIdForce(command.duelId());
        final var user = groupUser.second();
        final var group = groupUser.first();

        if (duel.acceptingPersonageId() != user.personageId()) {
            telegramSender.send(
                TelegramMethods.createAnswerCallbackQuery(
                    command.callbackId(),
                    Localization.get(group.language()).notDuelAcceptingPersonage()
                )
            );
            return;
        }

        if (duel.status() != DuelStatus.WAITING) {
            //TODO нормальный обработчик
            return;
        }

        duelService.finishDuel(duel.id());

        final var personage1 = personageService.getByIdForce(duel.initiatingPersonageId());
        final var personage2 = personageService.getByIdForce(duel.acceptingPersonageId());
        final var battlePersonage1 = personage1.toBattlePersonage();
        final var battlePersonage2 = personage2.toBattlePersonage();
        final var battleResult = twoPersonageTeamsBattle.battle(
            Collections.singletonList(battlePersonage1),
            Collections.singletonList(battlePersonage2)
        );

        final Personage winner;
        final Personage looser;
        if (battleResult instanceof TwoPersonageTeamsBattle.Result.FirstTeamWin) {
            winner = personage1;
            looser = personage2;
        } else {
            winner = personage2;
            looser = personage1;
        }

        duelService.addWinner(duel.id(), winner.id());
        final var duelEndTime = TimeUtils.moscowTime();
        personageService.changeHealth(
            personage1,
            battlePersonage1.health(),
            duelEndTime
        );
        personageService.changeHealth(
            personage2,
            battlePersonage2.health(),
            duelEndTime
        );

        telegramSender.send(
            TelegramMethods.createEditMessageText(
                group.id(),
                command.messageId(),
                Localization.get(group.language()).finishedDuel(winner, looser)
            )
        );
    }
}
