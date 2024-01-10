package ru.homyakin.seeker.telegram.command.group.duel;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.duel.DuelService;
import ru.homyakin.seeker.game.duel.models.DuelResult;
import ru.homyakin.seeker.game.duel.models.ProcessDuelError;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.group.stats.GroupStatsService;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.utils.models.Success;

@Component
public class AcceptDuelExecutor extends ProcessDuelExecutor<AcceptDuel> {
    private final DuelService duelService;
    private final GroupStatsService groupStatsService;
    private final UserService userService;

    public AcceptDuelExecutor(
        GroupUserService groupUserService,
        DuelService duelService,
        TelegramSender telegramSender,
        GroupStatsService groupStatsService,
        UserService userService
    ) {
        super(telegramSender, groupUserService);
        this.duelService = duelService;
        this.groupStatsService = groupStatsService;
        this.userService = userService;
    }

    @Override
    protected Either<ProcessDuelError, Success> processDuel(AcceptDuel command, Group group, User acceptor) {
        final var duel = duelService.getByIdForce(command.duelId());
        return duelService.finishDuel(duel, acceptor.personageId())
            .peek(result -> processDuelResult(result, acceptor, command, group))
            .map(result -> Success.INSTANCE);
    }

    private void processDuelResult(DuelResult result, User acceptor, AcceptDuel command, Group group) {
        final User winnerUser;
        final User loserUser;
        if (result.winner().personage().id().equals(acceptor.personageId())) {
            winnerUser = acceptor;
            loserUser = userService.getByPersonageIdForce(result.loser().personage().id());
        } else {
            winnerUser = userService.getByPersonageIdForce(result.winner().personage().id());
            loserUser = acceptor;
        }
        groupStatsService.increaseDuelsComplete(command.groupId(), winnerUser.personageId(), loserUser.personageId());

        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(group.id())
                .messageId(command.messageId())
                .text(finishedDuelText(group.language(), result, winnerUser, loserUser))
                .build()
        );
    }

    // TODO в локализацию
    private String finishedDuelText(
        Language language,
        DuelResult duelResult,
        User winnerUser,
        User loserUser
    ) {
        return DuelLocalization.finishedDuel(
            language,
            TgPersonageMention.of(duelResult.winner().personage(), winnerUser.id()),
            TgPersonageMention.of(duelResult.loser().personage(), loserUser.id())
        ) + "\n\n" +
            DuelLocalization.personageDuelResult(language, duelResult.winner(), true) + "\n" +
            DuelLocalization.personageDuelResult(language, duelResult.loser(), false);
    }
}
