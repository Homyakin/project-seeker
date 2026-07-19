package ru.homyakin.seeker.telegram.command.user.outpost;

import java.util.Optional;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class ConfirmOutpostDonateExecutor extends CommandExecutor<ConfirmOutpostDonate> {
    private final UserService userService;
    private final OutpostDonateTgService outpostDonateTgService;

    public ConfirmOutpostDonateExecutor(
        UserService userService,
        OutpostDonateTgService outpostDonateTgService
    ) {
        this.userService = userService;
        this.outpostDonateTgService = outpostDonateTgService;
    }

    @Override
    public void execute(ConfirmOutpostDonate command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        outpostDonateTgService.donate(
            user,
            command.building(),
            command.itemId(),
            Optional.of(command.messageId())
        );
    }
}
