package ru.homyakin.seeker.telegram.command.group.anomaly;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.anomaly.action.AnomalyService;
import ru.homyakin.seeker.game.outpost.action.OutpostService;
import ru.homyakin.seeker.locale.outpost.OutpostLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.OutpostKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class AnomalyBackOutpostExecutor extends CommandExecutor<AnomalyBackOutpost> {
    private final GroupUserService groupUserService;
    private final OutpostService outpostService;
    private final AnomalyService anomalyService;
    private final TelegramSender telegramSender;

    public AnomalyBackOutpostExecutor(
        GroupUserService groupUserService,
        OutpostService outpostService,
        AnomalyService anomalyService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.outpostService = outpostService;
        this.anomalyService = anomalyService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(AnomalyBackOutpost command) {
        final var group = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId()).first();
        final var domainGroupId = group.domainGroupId();
        final var slots = outpostService.listSlots(domainGroupId);
        final var text = OutpostLocalization.outpost(group.language(), slots, false);
        final var showAnomaly = anomalyService.isEligibleForMenu(domainGroupId);
        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(command.groupTgId())
                .messageId(command.messageId())
                .text(text)
                .keyboard(OutpostKeyboards.groupOutpostKeyboard(group.language(), showAnomaly))
                .build()
        );
        telegramSender.send(TelegramMethods.createAnswerCallbackQuery(command.callbackId(), ""));
    }
}
