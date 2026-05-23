package ru.homyakin.seeker.telegram.command.user.stats;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.v4.BattlePersonage;
import ru.homyakin.seeker.game.battle.v4.Position;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.locale.battle.BattleLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class BattleStatsExecutor extends CommandExecutor<BattleStats> {
    private final UserService userService;
    private final ItemService itemService;
    private final TelegramSender telegramSender;

    public BattleStatsExecutor(
        UserService userService,
        ItemService itemService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.itemService = itemService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(BattleStats command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var equippedItems = itemService.getEquippedItemsByPersonageIds(Set.of(user.personageId()))
            .getOrDefault(user.personageId(), List.of());
        final var battlePersonage = new BattlePersonage(equippedItems, Position.FRONT);
        final var text = BattleLocalization.battleStats(user.language(), battlePersonage, equippedItems);
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
            .build()
        );
    }
}
