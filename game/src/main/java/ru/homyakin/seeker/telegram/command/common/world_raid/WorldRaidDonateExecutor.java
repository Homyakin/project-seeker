package ru.homyakin.seeker.telegram.command.common.world_raid;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.world_raid.action.WorldRaidContributionService;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.personal.BulletinBoardLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class WorldRaidDonateExecutor extends CommandExecutor<WorldRaidDonate> {
    private final UserService userService;
    private final GroupTgService groupTgService;
    private final WorldRaidContributionService worldRaidContributionService;
    private final TelegramSender telegramSender;

    public WorldRaidDonateExecutor(
        UserService userService,
        GroupTgService groupTgService,
        WorldRaidContributionService worldRaidContributionService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.groupTgService = groupTgService;
        this.worldRaidContributionService = worldRaidContributionService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(WorldRaidDonate command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final Language language;
        if (command.isPrivate()) {
            language = user.language();
        } else {
            final var group = groupTgService.getOrCreate(GroupTgId.from(command.chatId()));
            language = group.language();
        }
        final var text = worldRaidContributionService.donate(user.personageId())
            .fold(
                error ->
                    BulletinBoardLocalization.worldRaidDonateError(language, error),
                money ->
                    BulletinBoardLocalization.successWorldRaidDonate(language, money)
            );
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(command.chatId())
            .text(text)
            .build()
        );
    }

}
