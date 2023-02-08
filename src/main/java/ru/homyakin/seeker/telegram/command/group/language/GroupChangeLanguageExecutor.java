package ru.homyakin.seeker.telegram.command.group.language;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class GroupChangeLanguageExecutor extends CommandExecutor<GroupChangeLanguage> {
    private final GroupService groupService;
    private final TelegramSender telegramSender;

    public GroupChangeLanguageExecutor(
        GroupService groupService,
        TelegramSender telegramSender
    ) {
        this.groupService = groupService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GroupChangeLanguage command) {
        final var group = groupService.getOrCreate(command.groupId());
        telegramSender.send(
            TelegramMethods.createSendMessage(
                command.groupId(),
                CommonLocalization.get(group.language()).chooseLanguage(),
                InlineKeyboards.languageKeyboard(group.language())
            )
        );
    }
}
