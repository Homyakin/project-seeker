package ru.homyakin.seeker.telegram.command.user.outpost;

import java.util.Optional;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.outpost.action.OutpostService;
import ru.homyakin.seeker.locale.outpost.OutpostLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class OutpostDonateItemExecutor extends CommandExecutor<OutpostDonateItem> {
    private final UserService userService;
    private final OutpostService outpostService;
    private final OutpostDonateTgService outpostDonateTgService;
    private final TelegramSender telegramSender;

    public OutpostDonateItemExecutor(
        UserService userService,
        OutpostService outpostService,
        OutpostDonateTgService outpostDonateTgService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.outpostService = outpostService;
        this.outpostDonateTgService = outpostDonateTgService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(OutpostDonateItem command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var item = outpostService.getDonatableItem(user.personageId(), command.itemId());
        if (item.isEmpty()) {
            outpostDonateTgService.donate(user, command.building(), command.itemId(), Optional.empty());
            return;
        }

        final var loadoutNames = outpostService.loadoutNamesForItem(user.personageId(), command.itemId());
        if (!loadoutNames.isEmpty()) {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(OutpostLocalization.confirmDonateLoadout(
                    user.language(),
                    item.get(),
                    outpostService.materialsForItem(item.get()),
                    loadoutNames
                ))
                .keyboard(InlineKeyboards.confirmOutpostDonateKeyboard(
                    user.language(),
                    command.building(),
                    command.itemId()
                ))
                .build()
            );
            return;
        }

        outpostDonateTgService.donate(user, command.building(), command.itemId(), Optional.empty());
    }
}
