package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.tavern_menu.OrderService;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemOrderError;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class ConsumeOrderExecutor extends CommandExecutor<ConsumeOrder> {
    private final GroupUserService groupUserService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;
    private final OrderService orderService;

    public ConsumeOrderExecutor(
        GroupUserService groupUserService,
        PersonageService personageService,
        TelegramSender telegramSender,
        OrderService orderService
    ) {
        this.groupUserService = groupUserService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
        this.orderService = orderService;
    }

    @Override
    public void execute(ConsumeOrder command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId());
        final var group = groupUser.first();
        final var consumer = personageService.getByIdForce(groupUser.second().personageId());
        orderService.consume(command.orderId(), consumer)
            .peek(
                item -> telegramSender.send(
                    EditMessageTextBuilder.builder()
                        .text(item.consumeText(group.language(), consumer))
                        .messageId(command.messageId())
                        .chatId(command.groupId())
                        .build()
                )
            )
            .peekLeft(
                error -> {
                    switch (error) {
                        case MenuItemOrderError.AlreadyFinalStatus ignored ->
                            telegramSender.send(
                                EditMessageTextBuilder
                                    .builder()
                                    .text(error.text(group.language()))
                                    .messageId(command.messageId())
                                    .chatId(command.groupId())
                                    .build()
                            );
                        case MenuItemOrderError.WrongConsumer ignored ->
                            telegramSender.send(
                                TelegramMethods.createAnswerCallbackQuery(command.callbackId(), error.text(group.language()))
                            );
                        case MenuItemOrderError.OrderLocked ignored -> {
                            telegramSender.send(
                                TelegramMethods.createAnswerCallbackQuery(command.callbackId(), error.text(group.language()))
                            );
                        }
                    }
                }
            );
    }
}
