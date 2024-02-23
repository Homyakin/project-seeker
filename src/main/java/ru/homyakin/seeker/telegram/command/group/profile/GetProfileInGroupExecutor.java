package ru.homyakin.seeker.telegram.command.group.profile;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GetProfileInGroupExecutor extends CommandExecutor<GetProfileInGroup> {
    private final GroupUserService groupUserService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public GetProfileInGroupExecutor(
        GroupUserService groupUserService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GetProfileInGroup command) {
        final var groupUserPair = groupUserService.getAndActivateOrCreate(
            command.groupId(),
            command.userId()
        );
        final var group = groupUserPair.first();
        final var user = groupUserPair.second();
        final var personage = personageService
            .getById(user.personageId())
            .orElseThrow(() -> new IllegalStateException("Personage must be present at user with groupId " + user))
            ;

        telegramSender.send(
            SendMessageBuilder.builder().chatId(command.groupId()).text(personage.shortProfile(group.language())).build()
        );
    }

}
