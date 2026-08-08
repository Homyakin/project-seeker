package ru.homyakin.seeker.telegram.command.common;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;

@Component
public class NoopCallbackExecutor extends CommandExecutor<NoopCallback> {
    private final TelegramSender telegramSender;

    public NoopCallbackExecutor(TelegramSender telegramSender) {
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(NoopCallback command) {
        telegramSender.send(
            AnswerCallbackQuery.builder()
                .callbackQueryId(command.callbackId())
                .build()
        );
    }
}
