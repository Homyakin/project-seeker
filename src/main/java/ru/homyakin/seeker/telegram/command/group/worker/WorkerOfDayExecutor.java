package ru.homyakin.seeker.telegram.command.group.worker;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.worker.action.WorkerOfDayCommand;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.worker.WorkerOfDayLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.game.worker.error.WorkerOfDayError;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class WorkerOfDayExecutor extends CommandExecutor<WorkerOfDay> {
    private final WorkerOfDayCommand workerOfDayCommand;
    private final GroupUserService groupUserService;
    private final PersonageService personageService;
    private final UserService userService;
    private final TelegramSender telegramSender;

    public WorkerOfDayExecutor(
        WorkerOfDayCommand workerOfDayCommand,
        GroupUserService groupUserService,
        PersonageService personageService,
        UserService userService,
        TelegramSender telegramSender
    ) {
        this.workerOfDayCommand = workerOfDayCommand;
        this.groupUserService = groupUserService;
        this.personageService = personageService;
        this.userService = userService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(WorkerOfDay command) {
        final var group = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId()).first();
        final var message = workerOfDayCommand
            .chooseInGroup(group.domainGroupId())
            .fold(
                error -> mapSpinErrorToMessage(error, group),
                result -> {
                    final var user = userService.getByPersonageIdForce(result.personage().id());
                    return WorkerOfDayLocalization.chosenUser(
                        group.language(),
                        TgPersonageMention.of(result.personage(), user.id()),
                        result.effect()
                    );
                }
            );

        telegramSender.send(SendMessageBuilder.builder()
            .chatId(command.groupId())
            .text(message)
            .build()
        );
    }

    private String mapSpinErrorToMessage(WorkerOfDayError error, GroupTg group) {
        return switch (error) {
            case WorkerOfDayError.NotEnoughUsers notEnoughUsers ->
                WorkerOfDayLocalization.notEnoughUsers(group.language(), notEnoughUsers.requiredUsers());
            case WorkerOfDayError.AlreadyChosen alreadyChosen -> {
                final var personage = personageService.getByIdForce(alreadyChosen.personageId());
                final var user = userService.getByPersonageIdForce(personage.id());
                yield WorkerOfDayLocalization.alreadyChosen(
                    group.language(), TgPersonageMention.of(personage, user.id())
                );
            }
            case WorkerOfDayError.InternalError _ -> CommonLocalization.internalError(group.language());
            case WorkerOfDayError.NotRegisteredGroup _ -> CommonLocalization.onlyForRegisteredGroup(group.language());
        };
    }
}
