package ru.homyakin.seeker.telegram;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllGroupChats;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeAllPrivateChats;
import ru.homyakin.seeker.telegram.command.type.CommandType;

@Configuration
public class TelegramBotCommandInitializer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final TelegramSender telegramSender;

    public TelegramBotCommandInitializer(TelegramSender telegramSender) {
        this.telegramSender = telegramSender;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initCommands() throws Exception {
        telegramSender.send(
            SetMyCommands.builder()
                .commands(GROUP_COMMANDS)
                .scope(new BotCommandScopeAllGroupChats())
                .build()
        );

        telegramSender.send(
            SetMyCommands.builder()
                .commands(PERSONAL_COMMAND)
                .scope(new BotCommandScopeAllPrivateChats())
                .build()
        );

        logger.info("Commands initialized");
    }

    private static final List<BotCommand> GROUP_COMMANDS = List.of(
        new BotCommand(CommandType.SHOW_HELP.getText(), "Get help"),
        new BotCommand(CommandType.CHANGE_LANGUAGE.getText(), "Change language in group"),
        new BotCommand(CommandType.TAVERN_MENU.getText(), "Get tavern menu"),
        new BotCommand(CommandType.START_DUEL.getText(), "Start duel with another player. Must be reply"),
        new BotCommand(CommandType.GET_PROFILE.getText(), "Get profile"),
        new BotCommand(CommandType.PERSONAGE_STATS.getText(), "Get my statistic"),
        new BotCommand(CommandType.GROUP_INFO.getText(), "Get group info"),
        new BotCommand(CommandType.WORKER_OF_DAY.getText(), "Get worker of the day"),
        new BotCommand(CommandType.TOP.getText(), "Get list of tops")
    );

    private static final List<BotCommand> PERSONAL_COMMAND = List.of(
        new BotCommand(CommandType.SHOW_HELP.getText(), "Get help"),
        new BotCommand(CommandType.RAID_REPORT.getText(), "Show last raid report"),
        new BotCommand(CommandType.INIT_FEEDBACK.getText(), "Send feedback to developers"),
        new BotCommand(CommandType.PERSONAGE_STATS.getText(), "Get my global statistic"),
        new BotCommand(CommandType.SETTINGS.getText(), "Get my settings"),
        new BotCommand(CommandType.WORLD_RAID_REPORT.getText(), "Show last world raid report")
    );
}
