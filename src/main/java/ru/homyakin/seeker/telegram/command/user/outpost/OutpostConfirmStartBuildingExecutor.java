package ru.homyakin.seeker.telegram.command.user.outpost;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.outpost.action.OutpostService;
import ru.homyakin.seeker.game.outpost.entity.OutpostApplyError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.outpost.OutpostLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.OutpostKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class OutpostConfirmStartBuildingExecutor extends CommandExecutor<OutpostConfirmStartBuilding> {
    private final UserService userService;
    private final OutpostService outpostService;
    private final GroupTgService groupTgService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public OutpostConfirmStartBuildingExecutor(
        UserService userService,
        OutpostService outpostService,
        GroupTgService groupTgService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.outpostService = outpostService;
        this.groupTgService = groupTgService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(OutpostConfirmStartBuilding command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var language = user.language();

        final var result = outpostService.tryApplyBuildOrUpgrade(user.personageId(), command.building());
        result.peek(applyResult -> {
            telegramSender.send(EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(command.messageId())
                .text(OutpostLocalization.startBuildingSuccessPrivate(language))
                .keyboard(OutpostKeyboards.emptyInlineKeyboard())
                .build()
            );
            final var personage = personageService.getByIdForce(user.personageId());
            final var groupTg = groupTgService.forceGet(applyResult.groupId());
            final var offer = applyResult.offer();
            final var materialsRequired = offer.building().materialsToReachLevel(offer.toLevel());
            final var groupText = OutpostLocalization.groupBuildingStarted(
                groupTg.language(),
                offer.building(),
                personage,
                offer.fromLevel(),
                offer.toLevel(),
                materialsRequired
            );
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(groupTg.id())
                .text(groupText)
                .build()
            );
        }).peekLeft(error -> {
            final var text = switch (error) {
                case OutpostApplyError.NoGroup _ -> OutpostLocalization.outpostNoGroup(language);
                case OutpostApplyError.NotAdmin _ -> OutpostLocalization.notAdminOutpost(language);
                case OutpostApplyError.NoOffer _, OutpostApplyError.Busy _ ->
                    OutpostLocalization.startBuildingConflict(language);
            };
            telegramSender.send(TelegramMethods.createAnswerCallbackQuery(
                command.callbackId(),
                text
            ));
            if (error instanceof OutpostApplyError.NotAdmin || error instanceof OutpostApplyError.NoGroup) {
                return;
            }
            telegramSender.send(EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(command.messageId())
                .text(text)
                .keyboard(OutpostKeyboards.emptyInlineKeyboard())
                .build()
            );
        });
    }
}
