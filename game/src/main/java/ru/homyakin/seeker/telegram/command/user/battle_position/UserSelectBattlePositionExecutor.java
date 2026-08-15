package ru.homyakin.seeker.telegram.command.user.battle_position;

import java.util.Optional;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class UserSelectBattlePositionExecutor extends CommandExecutor<UserSelectBattlePosition> {
    private final UserService userService;
    private final PersonageService personageService;
    private final UserChangeBattlePositionExecutor userChangeBattlePositionExecutor;

    public UserSelectBattlePositionExecutor(
        UserService userService,
        PersonageService personageService,
        UserChangeBattlePositionExecutor userChangeBattlePositionExecutor
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.userChangeBattlePositionExecutor = userChangeBattlePositionExecutor;
    }

    @Override
    public void execute(UserSelectBattlePosition command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        personageService.setBattlePosition(user.personageId(), command.position());
        userChangeBattlePositionExecutor.execute(
            new UserChangeBattlePosition(command.userId(), Optional.of(command.messageId()))
        );
    }
}
