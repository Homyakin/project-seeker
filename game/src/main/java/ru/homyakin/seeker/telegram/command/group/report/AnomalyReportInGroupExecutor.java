package ru.homyakin.seeker.telegram.command.group.report;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.anomaly.AnomalyLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class AnomalyReportInGroupExecutor extends CommandExecutor<AnomalyReportInGroup> {
    private final GroupUserService groupUserService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public AnomalyReportInGroupExecutor(
        GroupUserService groupUserService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(AnomalyReportInGroup command) {
        final var groupUserPair = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId());
        final var group = groupUserPair.first();
        final var user = groupUserPair.second();
        final var personage = personageService.getByIdForce(user.personageId());
        final var text = personage.memberGroupId()
            .filter(group.domainGroupId()::equals)
            .flatMap(memberGroupId -> personageService.getLastAnomalyResultInGroup(
                personage.id(),
                memberGroupId
            ))
            .map(result -> AnomalyLocalization.shortGroupReport(group.language(), result, personage))
            .orElseGet(() -> AnomalyLocalization.lastGroupAnomalyReportNotFound(group.language()));
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(group.id())
            .text(text)
            .replyMessageId(command.messageId())
            .build()
        );
    }
}
