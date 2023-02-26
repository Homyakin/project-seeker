package ru.homyakin.seeker.telegram;

import java.util.List;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllGroupChats;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllPrivateChats;
import ru.homyakin.seeker.telegram.command.type.CommandType;

@Configuration
public class BotInitializer {
    private final TelegramSender telegramSender;

    public BotInitializer(TelegramSender telegramSender) {
        this.telegramSender = telegramSender;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initCommands() throws Exception {
        telegramSender.execute(
            SetMyCommands.builder()
                .commands(GROUP_COMMANDS)
                .scope(new BotCommandScopeAllGroupChats())
                .build()
        );

        telegramSender.execute(
            SetMyCommands.builder()
                .commands(PERSONAL_COMMAND)
                .scope(new BotCommandScopeAllPrivateChats())
                .build()
        );
    }

    private static final List<BotCommand> GROUP_COMMANDS = List.of(
        new BotCommand(CommandType.HELP.getText(), "Get help"),
        new BotCommand(CommandType.CHANGE_LANGUAGE.getText(), "Change language in group"),
        new BotCommand(CommandType.TAVERN_MENU.getText(), "Get tavern menu"),
        new BotCommand(CommandType.START_DUEL.getText(), "Start duel with another player. Must be reply"),
        new BotCommand(CommandType.GET_PROFILE.getText(), "Get profile")
    );

    private static final List<BotCommand> PERSONAL_COMMAND = List.of(
        new BotCommand(CommandType.HELP.getText(), "Get help"),
        new BotCommand(CommandType.CHANGE_NAME.getText(), "Change name")
    );
}
