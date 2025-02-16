package ru.homyakin.seeker.telegram.command.group.management.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.EditGroupSettings;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group_settings.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class SetTimeZoneExecutor extends CommandExecutor<SetTimeZone> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final GroupUserService groupUserService;
    private final EditGroupSettings editGroupSettings;
    private final TelegramSender telegramSender;

    public SetTimeZoneExecutor(
        GroupUserService groupUserService,
        EditGroupSettings editGroupSettings,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.editGroupSettings = editGroupSettings;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(SetTimeZone command) {
        final var data = command.data();

        final var result = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId());
        final var group = result.first();
        if (data.isLeft()) {
            telegramSender.send(
                SendMessageBuilder
                    .builder()
                    .text(GroupManagementLocalization.incorrectSetTimeZoneCommand(group.language()))
                    .chatId(group.id())
                    .build()
            );
            return;
        }
        final var timeZone = data.get();

        if (!groupUserService.isUserAdminInGroup(command.groupId(), command.userId())) {
            logger.info("Not admin tried to toggle event interval");
            telegramSender.send(
                SendMessageBuilder.builder().text(CommonLocalization.onlyAdminAction(group.language())).chatId(group.id()).build()
            );
            return;
        }

        final var text = editGroupSettings.changeTimeZone(group.domainGroupId(), timeZone)
            .fold(
                error -> GroupManagementLocalization.incorrectTimeZone(group.language(), error),
                _ -> GroupManagementLocalization.successChangeTimeZone(group.language())
            );

        telegramSender.send(
            SendMessageBuilder.builder().chatId(group.id()).text(text).build()
        );
    }
}
