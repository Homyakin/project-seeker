package ru.homyakin.seeker.telegram.command.common;

import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.homyakin.seeker.telegram.command.Command;

public record NoopCallback(String callbackId) implements Command {
    public static NoopCallback from(CallbackQuery callback) {
        return new NoopCallback(callback.getId());
    }
}
