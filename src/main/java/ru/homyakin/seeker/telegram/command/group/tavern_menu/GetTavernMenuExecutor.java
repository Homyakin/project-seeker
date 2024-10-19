package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.tavern_menu.menu.MenuService;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GetTavernMenuExecutor extends CommandExecutor<GetTavernMenu> {
    private final GroupTgService groupTgService;
    private final MenuService menuService;
    private final TelegramSender telegramSender;

    public GetTavernMenuExecutor(
        GroupTgService groupTgService,
        MenuService menuService,
        TelegramSender telegramSender
    ) {
        this.groupTgService = groupTgService;
        this.menuService = menuService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GetTavernMenu command) {
        final var group = groupTgService.getOrCreate(command.groupId());
        final var menu = menuService.getAvailableMenu();
        telegramSender.send(
            SendMessageBuilder.builder().chatId(group.id()).text(menu.tavernMenuText(group.language())).build()
        );
    }

}
