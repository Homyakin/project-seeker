package ru.homyakin.seeker.telegram.command.common.world_raid;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.world_raid.action.JoinWorldRaidCommand;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.world_raid.WorldRaidLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class JoinWorldRaidExecutor extends CommandExecutor<JoinWorldRaid> {
    private final UserService userService;
    private final JoinWorldRaidCommand joinWorldRaidCommand;
    private final PersonageService personageService;
    private final ItemService itemService;
    private final TelegramSender telegramSender;

    public JoinWorldRaidExecutor(
        UserService userService,
        JoinWorldRaidCommand joinWorldRaidCommand,
        PersonageService personageService,
        ItemService itemService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.joinWorldRaidCommand = joinWorldRaidCommand;
        this.personageService = personageService;
        this.itemService = itemService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(JoinWorldRaid command) {
        final var user = userService.forceGetFromGroup(command.userId());
        final var result = joinWorldRaidCommand.execute(user.personageId());
        final var text = result.fold(
            error -> WorldRaidLocalization.joinError(user.language(), error),
            _ -> joinWorldRaidCallbackText(user.language(), user.personageId())
        );
        telegramSender.send(
            TelegramMethods.createAnswerCallbackQuery(command.callbackId(), text)
        );
    }

    private String joinWorldRaidCallbackText(Language language, PersonageId personageId) {
        final var builder = new StringBuilder(WorldRaidLocalization.successJoin(language));
        if (isBagFull(personageId)) {
            builder.append("\n").append(CommonLocalization.fullBagAlertOnRaidJoin(language));
        }
        return builder.toString();
    }

    private boolean isBagFull(PersonageId personageId) {
        final var personage = personageService.getByIdForce(personageId);
        return !personage.hasSpaceInBagForItems(itemService.getPersonageItems(personageId));
    }
}
