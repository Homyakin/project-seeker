package ru.homyakin.seeker.telegram.command.group.top;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.top.TopService;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class TopOutpostBuildSessionExecutor extends CommandExecutor<TopOutpostBuildSession> {
    private final TopService topService;
    private final GroupUserService groupUserService;
    private final TelegramSender telegramSender;

    public TopOutpostBuildSessionExecutor(
        TopService topService,
        GroupUserService groupUserService,
        TelegramSender telegramSender
    ) {
        this.topService = topService;
        this.groupUserService = groupUserService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(TopOutpostBuildSession command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId());
        final var group = groupUser.first();
        final var user = groupUser.second();
        final var language = group.language();
        final var text = command.parsed()
            .map(p -> textForParsed(group.domainGroupId(), language, user.personageId(), p))
            .orElseGet(() -> TopLocalization.topOutpostBuildInvalidUsage(language));
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(command.groupTgId())
            .text(text)
            .build()
        );
    }

    private String textForParsed(
        GroupId groupId,
        Language language,
        PersonageId requestedPersonageId,
        TopOutpostBuildSession.Parsed p
    ) {
        final var wrapped = topService.getTopOutpostBuildSession(
            groupId,
            p.building(),
            p.targetLevel()
        );
        return wrapped.toLocalizedString(language, requestedPersonageId);
    }
}
