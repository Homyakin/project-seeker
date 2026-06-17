package ru.homyakin.seeker.telegram.command.user.bulletin_board;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.world_raid.action.GetWorldRaidInfoCommand;
import ru.homyakin.seeker.locale.personal.BulletinBoardLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.telegram.world_raid.TelegramWorldRaidConfig;

@Component
public class ShowWorldRaidInfoExecutor extends CommandExecutor<ShowWorldRaidInfo> {
    private final UserService userService;
    private final GetWorldRaidInfoCommand getWorldRaidStatusCommand;
    private final TelegramWorldRaidConfig config;
    private final TelegramSender telegramSender;

    public ShowWorldRaidInfoExecutor(
        UserService userService,
        GetWorldRaidInfoCommand getWorldRaidInfoCommand,
        TelegramWorldRaidConfig config,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.getWorldRaidStatusCommand = getWorldRaidInfoCommand;
        this.config = config;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(ShowWorldRaidInfo command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var status = getWorldRaidStatusCommand.execute();
        final var language = user.language();
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(BulletinBoardLocalization.worldRaidInfo(language, status, config.getOrDefaultChannel(language)))
            .build()
        );
    }

}
