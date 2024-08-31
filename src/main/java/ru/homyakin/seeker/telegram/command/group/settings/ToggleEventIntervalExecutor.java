package ru.homyakin.seeker.telegram.command.group.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group_settings.GroupSettingsLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class ToggleEventIntervalExecutor extends CommandExecutor<ToggleEventInterval> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final GroupService groupService;
    private final GroupUserService groupUserService;
    private final TelegramSender telegramSender;

    public ToggleEventIntervalExecutor(
        GroupService groupService,
        GroupUserService groupUserService,
        TelegramSender telegramSender
    ) {
        this.groupService = groupService;
        this.groupUserService = groupUserService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(ToggleEventInterval command) {
        final var result = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId());
        final var group = result.first();

        if (!groupUserService.isUserAdminInGroup(command.groupId(), command.userId())) {
            logger.info("Not admin tried to toggle event interval");
            telegramSender.send(
                TelegramMethods.createAnswerCallbackQuery(
                    command.callbackId(),
                    CommonLocalization.onlyAdminAction(group.language())
                )
            );
            return;
        }

        groupService.toggleEventInterval(group, command.intervalIndex())
            .peek(
                updatedGroup -> telegramSender.send(
                    EditMessageTextBuilder
                        .builder()
                        .chatId(command.groupId())
                        .messageId(command.messageId())
                        .text(GroupSettingsLocalization.groupSettings(updatedGroup.language(), updatedGroup.settings()))
                        .keyboard(InlineKeyboards.eventIntervalsKeyboard(updatedGroup.language(), updatedGroup.settings().eventIntervals()))
                        .build()
                )
            )
            .peekLeft(
                _ -> telegramSender.send(
                    TelegramMethods.createAnswerCallbackQuery(
                        command.callbackId(),
                        GroupSettingsLocalization.zeroEnabledEventIntervals(group.language())
                    )
                )
            );
    }
}
