package ru.homyakin.seeker.telegram.command.user.stats;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class BattleStatsExecutor extends CommandExecutor<BattleStats> {
    private final UserService userService;
    private final BattleStatsMessageService battleStatsMessageService;

    public BattleStatsExecutor(
        UserService userService,
        BattleStatsMessageService battleStatsMessageService
    ) {
        this.userService = userService;
        this.battleStatsMessageService = battleStatsMessageService;
    }

    @Override
    public void execute(BattleStats command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        battleStatsMessageService.show(user, command.messageId());
    }
}
