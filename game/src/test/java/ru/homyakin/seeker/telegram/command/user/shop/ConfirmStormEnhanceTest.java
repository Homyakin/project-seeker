package ru.homyakin.seeker.telegram.command.user.shop;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.telegram.user.models.UserId;

class ConfirmStormEnhanceTest {
    @Test
    void from_parsesExpectedLevelAndRevision() {
        final var command = ConfirmStormEnhance.from(message("/senhance_42_7_11"));

        Assertions.assertEquals(new ConfirmStormEnhance(UserId.from(1), 42, 7, 11), command);
    }

    @Test
    void from_marksOldPayloadAsStale() {
        final var command = ConfirmStormEnhance.from(message("/senhance_42"));

        Assertions.assertEquals(new ConfirmStormEnhance(UserId.from(1), 42, -1, -1), command);
    }

    private static Message message(String text) {
        final var message = new Message();
        message.setFrom(new User(1L, "test", false));
        message.setText(text);
        return message;
    }
}
