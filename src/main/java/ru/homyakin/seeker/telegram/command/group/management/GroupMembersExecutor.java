package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

import java.util.stream.Collectors;

@Component
public class GroupMembersExecutor extends CommandExecutor<GroupMembers> {
    private final GroupTgService groupTgService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public GroupMembersExecutor(
            GroupTgService groupTgService,
            PersonageService personageService,
            TelegramSender telegramSender
    ) {
        this.groupTgService = groupTgService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GroupMembers command) {
        final var groupTg = groupTgService.getOrCreate(command.groupTgId());
        final var personages = personageService.getByGroupId(groupTg.domainGroupId());

        if (personages.isEmpty()) {
            telegramSender.send(
                    SendMessageBuilder.builder()
                            .chatId(command.groupTgId())
                            .text("В этой группе пока нет участников.")
                            .build()
            );
            return;
        }

        final var text = personages.stream()
                .map(this::formatPersonage)
                .collect(Collectors.joining("\n"));

        telegramSender.send(
                SendMessageBuilder.builder()
                        .chatId(command.groupTgId())
                        .text(text)
                        .build()
        );
    }

    private String formatPersonage(Personage personage) {
        return LocaleUtils.personageNameWithBadge(personage)
                + ": "
                + personage.energy().value()
                + " "
                + Icons.ENERGY;
    }
}
