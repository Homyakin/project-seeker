package ru.homyakin.seeker.telegram.command.group.spin;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.spin.EverydaySpinLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.EverydaySpinService;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.models.SpinError;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.utils.RandomUtils;

@Component
public class SpinExecutor extends CommandExecutor<Spin> {
    private final EverydaySpinService everydaySpinService;
    private final GroupUserService groupUserService;
    private final PersonageService personageService;
    private final UserService userService;
    private final TelegramSender telegramSender;

    public SpinExecutor(
        EverydaySpinService everydaySpinService,
        GroupUserService groupUserService,
        PersonageService personageService,
        UserService userService,
        TelegramSender telegramSender
    ) {
        this.everydaySpinService = everydaySpinService;
        this.groupUserService = groupUserService;
        this.personageService = personageService;
        this.userService = userService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(Spin command) {
        final var group = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId()).first();
        everydaySpinService.chooseRandomUserId(command.groupId())
            .map(userService::getOrCreateFromGroup)
            .peek(user -> {
                //TODO вынести награду в сервис
                final var personage = personageService.getByIdForce(user.personageId());
                final var reward = new Money(RandomUtils.getInInterval(MINIMUM_REWARD.value(), MAXIMUM_REWARD.value()));
                personageService.addMoney(personage, reward);
                telegramSender.send(SendMessageBuilder.builder()
                    .chatId(command.groupId())
                    .text(
                        EverydaySpinLocalization.chosenUser(group.language(), TgPersonageMention.of(personage, user.id()), reward)
                    )
                    .build()
                );
            })
            .peekLeft(error -> {
                    if (error instanceof SpinError.NotEnoughUsers notEnoughUsers) {
                        telegramSender.send(SendMessageBuilder.builder()
                            .chatId(command.groupId())
                            .text(EverydaySpinLocalization.notEnoughUsers(group.language(), notEnoughUsers.requiredUsers()))
                            .build()
                        );
                    } else if (error instanceof SpinError.AlreadyChosen alreadyChosen) {
                        final var personage = personageService.getByIdForce(
                            userService.getOrCreateFromGroup(alreadyChosen.userId()).personageId()
                        );
                        telegramSender.send(SendMessageBuilder.builder()
                            .chatId(command.groupId())
                            .text(
                                EverydaySpinLocalization.alreadyChosen(
                                    group.language(), TgPersonageMention.of(personage, alreadyChosen.userId())
                                )
                            )
                            .build()
                        );
                    }
                }
            );
    }

    private static final Money MINIMUM_REWARD = new Money(3);
    private static final Money MAXIMUM_REWARD = new Money(7);
}
