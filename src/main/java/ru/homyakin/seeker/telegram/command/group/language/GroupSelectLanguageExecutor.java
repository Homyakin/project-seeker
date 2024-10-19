package ru.homyakin.seeker.telegram.command.group.language;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;

@Component
public class GroupSelectLanguageExecutor extends CommandExecutor<GroupSelectLanguage> {
    private final GroupTgService groupTgService;
    private final TelegramSender telegramSender;

    public GroupSelectLanguageExecutor(
        GroupTgService groupTgService,
        TelegramSender telegramSender
    ) {
        this.groupTgService = groupTgService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GroupSelectLanguage command) {
        final var group = groupTgService.getOrCreate(command.groupId());
        final var updatedGroup = groupTgService.changeLanguage(group, command.language());
        telegramSender.send(EditMessageTextBuilder.builder()
            .chatId(command.groupId())
            .messageId(command.messageId())
            .text(CommonLocalization.chooseLanguage(updatedGroup.language()))
            .keyboard(InlineKeyboards.languageKeyboard(updatedGroup.language()))
            .build()
        );
    }

}

