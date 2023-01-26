package ru.homyakin.seeker.telegram.command.group.top;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class TopExecutor extends CommandExecutor<Top> {
    private final GroupUserService groupUserService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public TopExecutor(
        GroupUserService groupUserService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(Top command) {
        final var result = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId());
        final var group = result.first();
        final var user = result.second();
        final var topPersonages = personageService.getTopByExpInGroup(group.id(), 10);
        final var containsUser = topPersonages.stream().anyMatch(personage -> personage.id() == user.personageId());
        final StringBuilder topText = new StringBuilder();
        for (int i = 0; i < topPersonages.size(); ++i) {
            topText.append(i + 1).append(". ").append(topPersonages.get(i).toTopText()).append("\n");
        }
        if (!containsUser) {
            personageService.getPersonagePositionInTopByExpInGroup(user.personageId(), group.id()).ifPresent(position ->
                personageService.getById(user.personageId())
                    .map(Personage::toTopText)
                    .ifPresent(text -> {
                            topText.append(".........\n").append(position).append(". ").append(text);
                        }
                    )
            );
        }
        telegramSender.send(
            TelegramMethods.createSendMessage(
                group.id(),
                Localization.get(group.language()).topPersonagesByExpInGroup() + "\n" + topText
            )
        );
    }
}
