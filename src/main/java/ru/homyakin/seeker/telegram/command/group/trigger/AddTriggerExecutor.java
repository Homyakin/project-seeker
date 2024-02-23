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
public class AddTriggerExecutor extends CommandExecutor<AddTrigger> {

    private final GroupService groupService;
    private final TriggerService triggerService;
    private final TelegramSender telegramSender;

    public AddTriggerExecutor(
            GroupService groupService,
            TriggerService triggerService,
            TelegramSender telegramSender
    ) {
        this.groupService = groupService;
        this.triggerService = triggerService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(AddTrigger command) {
        final var group = groupService.getOrCreate(command.groupId());

        final var text = command
                .triggerText()
                .fold(
                        error -> mapTriggerCommandErrorToMessage(error, group),
                        triggerText -> triggerService
                                .createOrReplaceTrigger(group, command.textToTrigger(), triggerText)
                                .fold(
                                        error -> mapTriggerErrorToMessage(error, group),
                                        success -> TriggerLocalization.triggerCreated(group.language())
                                )
                );

        telegramSender.send(
                SendMessageBuilder.builder().text(text).chatId(group.id()).build()
        );

    }

    private String mapTriggerCommandErrorToMessage(TriggerCommandError error, Group group) {
        return switch (error) {
            case TriggerCommandError.NoTriggerTextCommandError ignored -> TriggerLocalization.noTriggerText(group.language());
            default -> TriggerLocalization.unknownException(group.language());
        };
    }

    private String mapTriggerErrorToMessage(TriggerError error, Group group) {
        return TriggerLocalization.unknownException(group.language());
    }
}


