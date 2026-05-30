package ru.homyakin.seeker.telegram.shop;

import java.util.Optional;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.contraband.action.ContrabandService;
import ru.homyakin.seeker.game.contraband.entity.Contraband;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.shop.ShopService;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.shop.ShopLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.ShopKeyboards;

@Component
public class ShopInlineTgService {
    private final ShopService shopService;
    private final ContrabandService contrabandService;
    private final TelegramSender telegramSender;

    public ShopInlineTgService(
        ShopService shopService,
        ContrabandService contrabandService,
        TelegramSender telegramSender
    ) {
        this.shopService = shopService;
        this.contrabandService = contrabandService;
        this.telegramSender = telegramSender;
    }

    public void showRandomBoxes(UserId userId, Language language, PersonageId personageId, int messageId) {
        final var items = shopService.getShopItems(personageId);
        sendEdit(
            userId,
            messageId,
            ShopLocalization.randomBoxesMenu(language, items, activeContraband(personageId)),
            ShopKeyboards.navigationKeyboard(language)
        );
    }

    public void showSlotObjects(
        UserId userId,
        Language language,
        int messageId,
        PersonageSlot slot
    ) {
        final var objects = shopService.getItemObjectsForSlot(slot);
        sendEdit(
            userId,
            messageId,
            ShopLocalization.slotObjectsMenu(
                language,
                slot,
                objects,
                shopService.specificObjectUnitPrice()
            ),
            ShopKeyboards.navigationKeyboard(language)
        );
    }

    private Optional<Contraband> activeContraband(PersonageId personageId) {
        return contrabandService.getActiveContraband(personageId);
    }

    private void sendEdit(
        UserId userId,
        int messageId,
        String text,
        org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard
    ) {
        telegramSender.send(EditMessageTextBuilder.builder()
            .chatId(userId)
            .messageId(messageId)
            .text(text)
            .keyboard(keyboard)
            .build()
        );
    }
}
