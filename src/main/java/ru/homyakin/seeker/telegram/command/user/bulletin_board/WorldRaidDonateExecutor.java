package ru.homyakin.seeker.telegram.command.user.bulletin_board;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.world_raid.action.WorldRaidContributionService;
import ru.homyakin.seeker.locale.personal.BulletinBoardLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class WorldRaidDonateExecutor extends CommandExecutor<WorldRaidDonate> {
    private final UserService userService;
    private final WorldRaidContributionService worldRaidContributionService;
    private final TelegramSender telegramSender;

    public WorldRaidDonateExecutor(
        UserService userService,
        WorldRaidContributionService worldRaidContributionService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.worldRaidContributionService = worldRaidContributionService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(WorldRaidDonate command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var text = worldRaidContributionService.donate(user.personageId())
            .fold(
                error ->
                    BulletinBoardLocalization.worldRaidDonateError(user.language(), error),
                money ->
                    BulletinBoardLocalization.successWorldRaidDonate(user.language(), money)
            );
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .build()
        );
    }

}
