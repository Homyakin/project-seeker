package ru.homyakin.seeker.telegram.command.user.bulletin_board;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.top.TopService;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class WorldRaidResearchTopExecutor extends CommandExecutor<WorldRaidResearchTop> {
    private final UserService userService;
    private final TopService topService;
    private final TelegramSender telegramSender;

    public WorldRaidResearchTopExecutor(
        UserService userService,
        TopService topService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.topService = topService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(WorldRaidResearchTop command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var top = topService.getTopWorldRaidResearch();
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(top.toLocalizedString(user.language(), user.personageId()))
            .build()
        );
    }

}
