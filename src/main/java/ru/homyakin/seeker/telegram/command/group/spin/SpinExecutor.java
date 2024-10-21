package ru.homyakin.seeker.telegram.command.group.spin;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.spin.action.ChooseRandomPersonage;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.spin.EverydaySpinLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.game.spin.error.SpinError;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class SpinExecutor extends CommandExecutor<Spin> {
    private final ChooseRandomPersonage chooseRandomPersonage;
    private final GroupUserService groupUserService;
    private final PersonageService personageService;
    private final UserService userService;
    private final TelegramSender telegramSender;

    public SpinExecutor(
        ChooseRandomPersonage chooseRandomPersonage,
        GroupUserService groupUserService,
        PersonageService personageService,
        UserService userService,
        TelegramSender telegramSender
    ) {
        this.chooseRandomPersonage = chooseRandomPersonage;
        this.groupUserService = groupUserService;
        this.personageService = personageService;
        this.userService = userService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(Spin command) {
        final var group = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId()).first();
        final var message = chooseRandomPersonage
            .chooseInGroup(group.domainGroupId())
            .fold(
                error -> mapSpinErrorToMessage(error, group),
                personageId -> {
                    final var personage = personageService.getByIdForce(personageId);
                    final var user = userService.getByPersonageIdForce(personageId);
                    return EverydaySpinLocalization.chosenUser(group.language(), TgPersonageMention.of(personage, user.id()));
                }
            );

        telegramSender.send(SendMessageBuilder.builder()
            .chatId(command.groupId())
            .text(message)
            .build()
        );
    }

    private String mapSpinErrorToMessage(SpinError error, GroupTg group) {
        return switch (error) {
            case SpinError.NotEnoughUsers notEnoughUsers ->
                EverydaySpinLocalization.notEnoughUsers(group.language(), notEnoughUsers.requiredUsers());
            case SpinError.AlreadyChosen alreadyChosen -> {
                final var personage = personageService.getByIdForce(alreadyChosen.personageId());
                final var user = userService.getByPersonageIdForce(personage.id());
                yield EverydaySpinLocalization.alreadyChosen(
                    group.language(), TgPersonageMention.of(personage, user.id())
                );
            }
            case SpinError.InternalError _ -> CommonLocalization.internalError(group.language());
        };
    }
}
