package ru.homyakin.seeker.telegram.command.common.help;

import org.springframework.stereotype.Component;

import ru.homyakin.seeker.game.valentine.entity.ValentineConfig;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.valentine.ValentineLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class HelpLoveExecutor extends CommandExecutor<HelpLove> {
    private final UserService userService;
    private final GroupTgService groupTgService;
    private final TelegramSender telegramSender;
    private final ValentineConfig valentineConfig;

    public HelpLoveExecutor(
        UserService userService,
        GroupTgService groupTgService,
        TelegramSender telegramSender,
        ValentineConfig valentineConfig
    ) {
        this.userService = userService;
        this.groupTgService = groupTgService;
        this.telegramSender = telegramSender;
        this.valentineConfig = valentineConfig;
    }

    @Override
    public void execute(HelpLove command) {
        final Language language;
        final var builder = SendMessageBuilder.builder();
        if (command.isPrivate()) {
            final var user = userService.forceGetFromPrivate(UserId.from(command.chatId()));
            builder.chatId(user.id());
            language = user.language();
        } else {
            final var group = groupTgService.getOrCreate(GroupTgId.from(command.chatId()));
            builder.chatId(group.id());
            language = group.language();
        }
        telegramSender.send(
            builder.text(ValentineLocalization.helpLove(language, valentineConfig))
                .build()
        );
    }
}
