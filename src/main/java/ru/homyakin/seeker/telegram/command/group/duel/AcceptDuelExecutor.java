package ru.homyakin.seeker.telegram.command.group.duel;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.TwoPersonageTeamsBattle;
import ru.homyakin.seeker.game.duel.DuelService;
import ru.homyakin.seeker.game.duel.models.DuelStatus;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

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
                    DuelLocalization.notDuelAcceptingPersonage(group.language())
                )
            );
            return;
        }

        if (duel.status() != DuelStatus.WAITING) {
            //TODO нормальный обработчик
            return;
        }

        final var result = duelService.finishDuel(duel.id());
        if (result.isLeft()) {
            telegramSender.send(
                TelegramMethods.createAnswerCallbackQuery(
                    command.callbackId(),
                    DuelLocalization.notEnoughMoneyAtAccepting(group.language(), result.getLeft().money())
                )
            );
            return;
        }

        // TODO вынести в отдельный поток и сервис
        final var personage1 = personageService.getByIdForce(duel.initiatingPersonageId());
        final var personage2 = personageService.getByIdForce(duel.acceptingPersonageId());
        final var battlePersonage1 = personage1.toBattlePersonage();
        final var battlePersonage2 = personage2.toBattlePersonage();
        final var battleResult = twoPersonageTeamsBattle.battle(
            new ArrayList<>(List.of(battlePersonage1)),
            new ArrayList<>(List.of(battlePersonage2))
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

        telegramSender.send(
            TelegramMethods.createEditMessageText(
                group.id(),
                command.messageId(),
                DuelLocalization.finishedDuel(group.language(), winner, looser)
            )
        );
    }
}
