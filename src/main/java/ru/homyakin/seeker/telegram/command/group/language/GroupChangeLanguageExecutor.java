package ru.homyakin.seeker.telegram.command.group.language;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GroupChangeLanguageExecutor extends CommandExecutor<GroupChangeLanguage> {
    private final GroupTgService groupTgService;
    private final TelegramSender telegramSender;

    public GroupChangeLanguageExecutor(
        GroupTgService groupTgService,
        TelegramSender telegramSender
    ) {
        this.groupTgService = groupTgService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GroupChangeLanguage command) {
        final var group = groupTgService.getOrCreate(command.groupId());
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(command.groupId())
            .text(CommonLocalization.chooseLanguage(group.language()))
            .keyboard(InlineKeyboards.languageKeyboard(group.language()))
            .build()
        );
    }
}
