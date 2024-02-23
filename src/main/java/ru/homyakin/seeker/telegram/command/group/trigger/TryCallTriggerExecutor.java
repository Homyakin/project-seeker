package ru.homyakin.seeker.telegram.command.group.trigger;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.group.TriggerService;
import ru.homyakin.seeker.telegram.group.models.Trigger;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

import java.util.Optional;

@Component
public class TryCallTriggerExecutor extends CommandExecutor<TryCallTrigger> {

    private final GroupService groupService;
    private final TriggerService triggerService;
    private final TelegramSender telegramSender;

    public TryCallTriggerExecutor(
            GroupService groupService,
            TriggerService triggerService,
            TelegramSender telegramSender
    ) {
        this.groupService = groupService;
        this.triggerService = triggerService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(TryCallTrigger command) {
        final var group = groupService.getOrCreate(command.groupId());

        Optional<Trigger> optionalTrigger = triggerService.getTrigger(group, command.possibleTextToTrigger());

        optionalTrigger.ifPresent(trigger -> telegramSender.send(
                SendMessageBuilder.builder().text(trigger.triggerText()).chatId(group.id()).build()
        ));

    }
}