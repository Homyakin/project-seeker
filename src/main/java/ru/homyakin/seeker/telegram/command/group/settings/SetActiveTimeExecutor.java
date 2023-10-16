package ru.homyakin.seeker.telegram.command.group.settings;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group_settings.ActiveTimeLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.models.ActiveTimeError;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class SetActiveTimeExecutor extends CommandExecutor<SetActiveTime> {
    private final GroupService groupService;
    private final GroupUserService groupUserService;
    private final TelegramSender telegramSender;

    public SetActiveTimeExecutor(
        GroupService groupService,
        GroupUserService groupUserService,
        TelegramSender telegramSender
    ) {
        this.groupService = groupService;
        this.groupUserService = groupUserService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(SetActiveTime command) {
        final var result = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId());
        final var group = result.first();

        final var userAdminResult = groupUserService.isUserAdminInGroup(command.groupId(), command.userId());
        if (userAdminResult.isRight() && !userAdminResult.get()) {
            telegramSender.send(
                SendMessageBuilder.builder().text(CommonLocalization.onlyAdminLanguage(group.language())).chatId(group.id()).build()
            );
            return;
        } else if (userAdminResult.isLeft()) {
            telegramSender.send(
                SendMessageBuilder.builder().text(CommonLocalization.internalError(group.language())).chatId(group.id()).build()
            );
            return;
        }

        final var text = command
            .info()
            .fold(
                error -> mapActiveTimeCommandErrorToMessage(error, group),
                info -> groupService
                    .updateActiveTime(group, info.startHour(), info.endHour(), info.timeZone())
                    .fold(
                        error -> mapActiveTimeErrorToMessage(error, group),
                        success -> ActiveTimeLocalization.successChange(group.language())
                    )
            );

        telegramSender.send(
            SendMessageBuilder.builder().text(text).chatId(group.id()).build()
        );
    }

    private String mapActiveTimeErrorToMessage(ActiveTimeError error, Group group) {
        return switch (error) {
            case ActiveTimeError.IncorrectTimeZone incorrectTimeZone ->
                ActiveTimeLocalization.incorrectTimeZone(
                    group.language(),
                    incorrectTimeZone.min(),
                    incorrectTimeZone.max()
                );
            case ActiveTimeError.IncorrectHour ignored ->
                ActiveTimeLocalization.incorrectHour(group.language());
            case ActiveTimeError.StartMoreThanEnd ignored ->
                ActiveTimeLocalization.startMoreThanEnd(group.language());
        };
    }

    private String mapActiveTimeCommandErrorToMessage(ActiveTimeCommandError error, Group group) {
        return switch (error) {
            case ActiveTimeCommandError.ArgumentsNotANumber ignored ->
                ActiveTimeLocalization.argumentsNotANumber(group.language());
            case ActiveTimeCommandError.IncorrectArgumentsNumber ignored ->
                ActiveTimeLocalization.incorrectArgumentsNumber(group.language());
        };
    }
}
