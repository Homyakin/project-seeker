package ru.homyakin.seeker.telegram;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.telegram.utils.TelegramUtils;

public class TelegramUtilsTest {
    private static final String BOT_USERNAME = "TestBot";
    private static final String SIMPLE_COMMAND = "/test";
    private static final String MENTION_COMMAND = SIMPLE_COMMAND + "@" + BOT_USERNAME;

    @Test
    @DisplayName("When simple bot command, then recognize as bot command")
    public void testSimpleBotCommand() {
        Assertions.assertTrue(TelegramUtils.isBotCommand(SIMPLE_COMMAND, BOT_USERNAME));
    }

    @Test
    @DisplayName("When simple bot command with text, then recognize as bot command")
    public void testSimpleBotCommandWithText() {
        final var command = SIMPLE_COMMAND + " text";
        Assertions.assertTrue(TelegramUtils.isBotCommand(command, BOT_USERNAME));
    }

    @Test
    @DisplayName("When simple bot command with mention text, then recognize as bot command")
    public void testSimpleBotCommandWithMentionText() {
        final var command = SIMPLE_COMMAND + " @mention";
        Assertions.assertTrue(TelegramUtils.isBotCommand(command, BOT_USERNAME));
    }

    @Test
    @DisplayName("When mention bot command, then recognize as bot command")
    public void testMentionBotCommand() {
        Assertions.assertTrue(TelegramUtils.isBotCommand(MENTION_COMMAND, BOT_USERNAME));
    }

    @Test
    @DisplayName("When mention bot command with text, then recognize as bot command")
    public void testMentionBotCommandWithText() {
        final var command = MENTION_COMMAND + " text";
        Assertions.assertTrue(TelegramUtils.isBotCommand(command, BOT_USERNAME));
    }

    @Test
    @DisplayName("When mention bot command with mention text, then recognize as bot command")
    public void testMentionBotCommandWithMentionText() {
        final var command = MENTION_COMMAND + " @mention";
        Assertions.assertTrue(TelegramUtils.isBotCommand(command, BOT_USERNAME));
    }

    @Test
    @DisplayName("When mention not this bot command, then not recognize as bot command")
    public void testMentionNotThisBotCommand() {
        final var command = SIMPLE_COMMAND + "@mentionbot";
        Assertions.assertFalse(TelegramUtils.isBotCommand(command, BOT_USERNAME));
    }

    @Test
    @DisplayName("When mention not this bot command with mention this bot text, then not recognize as bot command")
    public void testMentionNotThisBotCommandWithMentionThisBotText() {
        final var command = SIMPLE_COMMAND + "@mentionbot " + BOT_USERNAME;
        Assertions.assertFalse(TelegramUtils.isBotCommand(command, BOT_USERNAME));
    }
}
