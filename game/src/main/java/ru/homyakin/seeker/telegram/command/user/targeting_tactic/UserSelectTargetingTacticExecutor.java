package ru.homyakin.seeker.telegram.command.user.targeting_tactic;

import java.util.Optional;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class UserSelectTargetingTacticExecutor extends CommandExecutor<UserSelectTargetingTactic> {
    private final UserService userService;
    private final PersonageService personageService;
    private final UserChangeTargetingTacticExecutor userChangeTargetingTacticExecutor;

    public UserSelectTargetingTacticExecutor(
        UserService userService,
        PersonageService personageService,
        UserChangeTargetingTacticExecutor userChangeTargetingTacticExecutor
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.userChangeTargetingTacticExecutor = userChangeTargetingTacticExecutor;
    }

    @Override
    public void execute(UserSelectTargetingTactic command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        personageService.setTargetingTactic(user.personageId(), command.targetingTactic());
        userChangeTargetingTacticExecutor.execute(
            new UserChangeTargetingTactic(command.userId(), Optional.of(command.messageId()))
        );
    }
}
