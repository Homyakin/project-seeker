package ru.homyakin.seeker.telegram.command.group.settings;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group_settings.ActiveTimeLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.models.ActiveTimeError;
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
        final var text = command.info()
            .map(info -> groupService
                .updateActiveTime(group, info.startHour(), info.endHour(), info.timeZone())
                .map(it -> ActiveTimeLocalization.successChange(group.language()))
                .getOrElseGet(
                    error -> {
                        if (error instanceof ActiveTimeError.IncorrectTimeZone incorrectTimeZone) {
                            return ActiveTimeLocalization.incorrectTimeZone(
                                group.language(),
                                incorrectTimeZone.min(),
                                incorrectTimeZone.max()
                            );
                        } else if (error instanceof ActiveTimeError.IncorrectHour) {
                            return ActiveTimeLocalization.incorrectHour(group.language());
                        } else if (error instanceof ActiveTimeError.StartMoreThanEnd) {
                            return ActiveTimeLocalization.startMoreThanEnd(group.language());
                        }
                        //TODO новый switch
                        throw new IllegalStateException();
                    }
                )
            )
            .getOrElseGet(error -> {
                if (error instanceof ActiveTimeCommandError.IncorrectArgumentsNumber) {
                    return ActiveTimeLocalization.incorrectArgumentsNumber(group.language());
                } else if (error instanceof ActiveTimeCommandError.ArgumentsNotANumber) {
                    return ActiveTimeLocalization.argumentsNotANumber(group.language());
                }
                throw new IllegalStateException();
            });

        telegramSender.send(
            SendMessageBuilder.builder().text(text).chatId(group.id()).build()
        );
    }
}
