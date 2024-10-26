package ru.homyakin.seeker.telegram.command.group.settings;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.ChangeGroupNameCommand;
import ru.homyakin.seeker.game.utils.NameError;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group_settings.GroupSettingsLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class ChangeGroupNameExecutor extends CommandExecutor<ChangeGroupName> {
    private final GroupTgService groupTgService;
    private final GroupUserService groupUserService;
    private final ChangeGroupNameCommand changeGroupNameCommand;
    private final TelegramSender telegramSender;

    public ChangeGroupNameExecutor(
        GroupTgService groupTgService,
        GroupUserService groupUserService,
        ChangeGroupNameCommand changeGroupNameCommand,
        TelegramSender telegramSender
    ) {
        this.groupTgService = groupTgService;
        this.groupUserService = groupUserService;
        this.changeGroupNameCommand = changeGroupNameCommand;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(ChangeGroupName command) {
        final var groupTg = groupTgService.getOrCreate(command.groupTgId());
        if (command.name().isEmpty()) {
            telegramSender.send(
                SendMessageBuilder
                    .builder()
                    .chatId(command.groupTgId())
                    .text(GroupSettingsLocalization.changeNameInvalidFormat(groupTg.language()))
                    .build()
            );
            return;
        }
        final var isUserAdmin = groupUserService.isUserAdminInGroup(command.groupTgId(), command.userId());
        if (!isUserAdmin) {
            telegramSender.send(
                SendMessageBuilder
                    .builder()
                    .chatId(command.groupTgId())
                    .text(CommonLocalization.onlyAdminAction(groupTg.language()))
                    .build()
            );
            return;
        }
        final var text = changeGroupNameCommand.execute(groupTg.domainGroupId(), command.name().get())
            .fold(
                error -> switch (error) {
                    case NameError.InvalidLength invalidLength -> GroupSettingsLocalization.changeNameInvalidLength(
                        groupTg.language(),
                        invalidLength
                    );
                    case NameError.NotAllowedSymbols _ -> GroupSettingsLocalization.changeNameInvalidSymbols(groupTg.language());
                },
                _ -> GroupSettingsLocalization.successChangeName(groupTg.language(), command.name().get())
            );
        telegramSender.send(
            SendMessageBuilder
                .builder()
                .chatId(command.groupTgId())
                .text(text)
                .build()
        );
    }
}
