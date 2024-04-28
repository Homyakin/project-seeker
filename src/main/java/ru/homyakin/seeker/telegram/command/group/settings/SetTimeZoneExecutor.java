package ru.homyakin.seeker.telegram.command.group.settings;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group_settings.GroupSettingsLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class SetTimeZoneExecutor extends CommandExecutor<SetTimeZone> {
    private final GroupService groupService;
    private final GroupUserService groupUserService;
    private final TelegramSender telegramSender;

    public SetTimeZoneExecutor(
        GroupService groupService,
        GroupUserService groupUserService,
        TelegramSender telegramSender
    ) {
        this.groupService = groupService;
        this.groupUserService = groupUserService;
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
                    .text(GroupSettingsLocalization.incorrectSetTimeZoneCommand(group.language()))
                    .chatId(group.id())
                    .build()
            );
            return;
        }
        final var timeZone = data.get();

        if (!groupUserService.isUserAdminInGroup(command.groupId(), command.userId())) {
            telegramSender.send(
                SendMessageBuilder.builder().text(CommonLocalization.onlyAdminAction(group.language())).chatId(group.id()).build()
            );
            return;
        }

        final var text = groupService.changeTimeZone(group, timeZone)
            .fold(
                error -> GroupSettingsLocalization.incorrectTimeZone(group.language(), error),
                updatedGroup -> GroupSettingsLocalization.successChangeTimeZone(updatedGroup.language())
            );

        telegramSender.send(
            SendMessageBuilder.builder().chatId(group.id()).text(text).build()
        );
    }
}
