package ru.homyakin.seeker.telegram.command.common.help;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.help.HelpLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class ShowHelpExecutor extends CommandExecutor<ShowHelp> {
    private final UserService userService;
    private final GroupService groupService;
    private final TelegramSender telegramSender;

    public ShowHelpExecutor(UserService userService, GroupService groupService, TelegramSender telegramSender) {
        this.userService = userService;
        this.groupService = groupService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(ShowHelp command) {
        final Language language;
        if (command.isPrivate()) {
            language = userService.getOrCreateFromPrivate(command.chatId()).language();
        } else {
            language = groupService.getOrCreate(command.chatId()).language();
        }
        telegramSender.send(
            TelegramMethods.createSendMessage(
                command.chatId(),
                HelpLocalization.main(language),
                InlineKeyboards.helpKeyboard(language)
            )
        );
    }
}
