package ru.homyakin.seeker.telegram.command.user.stats;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.battle.BattleLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class BattleStatsMessageService {
    private final PersonageService personageService;
    private final ItemService itemService;
    private final TelegramSender telegramSender;

    public BattleStatsMessageService(
        PersonageService personageService,
        ItemService itemService,
        TelegramSender telegramSender
    ) {
        this.personageService = personageService;
        this.itemService = itemService;
        this.telegramSender = telegramSender;
    }

    public void show(User user, Optional<Integer> messageId) {
        final var text = battleStatsText(user);
        final var keyboard = InlineKeyboards.battleStatsKeyboard(user.language());
        if (messageId.isPresent()) {
            telegramSender.send(EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(messageId.get())
                .text(text)
                .keyboard(keyboard)
                .build()
            );
        } else {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(text)
                .keyboard(keyboard)
                .build()
            );
        }
    }

    private String battleStatsText(User user) {
        final var personage = personageService.getByIdForce(user.personageId());
        final var equippedItems = itemService.getEquippedItemsByPersonageIds(Set.of(user.personageId()))
            .getOrDefault(user.personageId(), List.of());
        final var battlePersonage = new BattlePersonage(equippedItems, personage.position());
        battlePersonage.setTargetingTactic(personage.targetingTactic());
        return BattleLocalization.battleStats(user.language(), battlePersonage, equippedItems);
    }
}
