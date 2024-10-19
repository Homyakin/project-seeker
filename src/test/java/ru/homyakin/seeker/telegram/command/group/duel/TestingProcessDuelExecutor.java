package ru.homyakin.seeker.telegram.command.group.duel;

import io.vavr.control.Either;
import ru.homyakin.seeker.game.duel.models.ProcessDuelError;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.utils.models.Success;

public class TestingProcessDuelExecutor extends ProcessDuelExecutor {

    protected TestingProcessDuelExecutor(TelegramSender telegramSender, GroupUserService groupUserService) {
        super(telegramSender, groupUserService);
    }

    @Override
    protected Either<ProcessDuelError, Success> processDuel(ProcessDuel command, GroupTg group, User acceptor) {
        return switch ((int) command.duelId()) {
            case 1 -> Either.left(ProcessDuelError.DuelLocked.INSTANCE);
            case 2 -> Either.left(ProcessDuelError.DuelIsFinished.INSTANCE);
            case 3 -> Either.left(ProcessDuelError.NotDuelAcceptor.INSTANCE);
            default -> throw new IllegalArgumentException("Incorrect test state for stub");
        };
    }
}
