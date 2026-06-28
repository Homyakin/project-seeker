package ru.homyakin.seeker.telegram.command.user.outpost;

import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.outpost.action.OutpostService;
import ru.homyakin.seeker.game.outpost.entity.OutpostSlot;
import ru.homyakin.seeker.game.outpost.entity.OutpostSlotAccessError;
import ru.homyakin.seeker.game.shop.ShopConfig;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.locale.outpost.OutpostLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.OutpostKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class OutpostBuildingContributeViewExecutor extends CommandExecutor<OutpostBuildingContributeView> {
    private final UserService userService;
    private final OutpostService outpostService;
    private final ItemService itemService;
    private final ShopConfig shopConfig;
    private final TelegramSender telegramSender;

    public OutpostBuildingContributeViewExecutor(
        UserService userService,
        OutpostService outpostService,
        ItemService itemService,
        ShopConfig shopConfig,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.outpostService = outpostService;
        this.itemService = itemService;
        this.shopConfig = shopConfig;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(OutpostBuildingContributeView command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var language = user.language();
        final var building = command.building();
        final var slotResult = outpostService.slotForBuilding(user.personageId(), building);
        if (slotResult.isLeft() && slotResult.getLeft() == OutpostSlotAccessError.NoGroup.INSTANCE) {
            telegramSender.send(TelegramMethods.createAnswerCallbackQuery(
                command.callbackId(),
                OutpostLocalization.outpostNoGroup(language)
            ));
            return;
        }
        final var slot = slotResult.isRight() ? slotResult.get() : OutpostSlot.EmptySlot.INSTANCE;

        final var occupiedOpt = slot instanceof OutpostSlot.BuildingSlot o
            ? Optional.of(o)
            : Optional.<OutpostSlot.BuildingSlot>empty();
        final var progressOpt = occupiedOpt.flatMap(OutpostSlot.BuildingSlot::progress);
        if (progressOpt.isEmpty()) {
            final var menuText = OutpostLocalization.buildingMenu(language, building, slot);
            telegramSender.send(EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(command.messageId())
                .text(menuText)
                .keyboard(OutpostKeyboards.outpostBuildingMainMenuKeyboard(language, building, slot))
                .build()
            );
            return;
        }

        final var occupied = occupiedOpt.get();
        final var progress = progressOpt.get();
        final var bagItems = itemService.getPersonageItems(user.personageId()).items().stream()
            .filter(i -> !i.isEquipped())
            .sorted(ItemLocalization::itemComparator)
            .toList();
        final var itemsBlock = bagItems.isEmpty()
            ? OutpostLocalization.buildingContributeEmptyBag(language)
            : bagItems.stream()
                .map(item -> {
                    final var materials = shopConfig.buyingPriceByRarity(item.rarity()).value();
                    final var donateCommand = CommandType.OUTPOST_DONATE_ITEM.getText()
                        + TextConstants.TG_COMMAND_DELIMITER
                        + building.id()
                        + TextConstants.TG_COMMAND_DELIMITER
                        + item.id();
                    return OutpostLocalization.buildingContributeItemLine(language, item, materials, donateCommand);
                })
                .collect(Collectors.joining("\n"));

        final var text = OutpostLocalization.buildingContributePicker(language, occupied, progress, itemsBlock);
        telegramSender.send(EditMessageTextBuilder.builder()
            .chatId(user.id())
            .messageId(command.messageId())
            .text(text)
            .keyboard(OutpostKeyboards.outpostBuildingContributePickerKeyboard(language, building))
            .build()
        );
    }
}
