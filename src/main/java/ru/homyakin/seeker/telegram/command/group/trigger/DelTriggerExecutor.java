package ru.homyakin.seeker.telegram.command.group.trigger;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.trigger.TriggerLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.group.TriggerService;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.group.models.TriggerError;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class DelTriggerExecutor extends CommandExecutor<DelTrigger> {

    private final GroupService groupService;
    private final TriggerService triggerService;
    private final TelegramSender telegramSender;

    public DelTriggerExecutor(
            GroupService groupService,
            TriggerService triggerService,
            TelegramSender telegramSender
    ) {
        this.groupService = groupService;
        this.triggerService = triggerService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(DelTrigger command) {
        final var group = groupService.getOrCreate(command.groupId());

        final var text = triggerService
                .deleteTrigger(group, command.textToTrigger())
                .fold(
                        error -> mapTriggerErrorToMessage(error, group),
                        success -> TriggerLocalization.triggerDeleted(group.language())
                );

        telegramSender.send(
                SendMessageBuilder.builder().text(text).chatId(group.id()).build()
        );

    }

    private String mapTriggerErrorToMessage(TriggerError error, Group group) {
        return switch (error) {
            case TriggerError.NoTriggerFound ignored -> TriggerLocalization.triggerNotFound(group.language());
            default -> TriggerLocalization.unknownException(group.language());
        };
    }
}