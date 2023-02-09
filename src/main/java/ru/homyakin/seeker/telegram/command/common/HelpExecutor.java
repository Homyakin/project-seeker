package ru.homyakin.seeker.telegram.command.common;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class HelpExecutor extends CommandExecutor<Help> {
    private final UserService userService;
    private final GroupService groupService;
    private final TelegramSender telegramSender;

    public HelpExecutor(UserService userService, GroupService groupService, TelegramSender telegramSender) {
        this.userService = userService;
        this.groupService = groupService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(Help command) {
        final Language language;
        if (command.isPrivate()) {
            language = userService.getOrCreateFromPrivate(command.chatId()).language();
        } else {
            language = groupService.getOrCreate(command.chatId()).language();
        }
        telegramSender.send(TelegramMethods.createSendMessage(command.chatId(), CommonLocalization.help(language)));
    }
}
