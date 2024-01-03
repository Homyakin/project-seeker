package ru.homyakin.seeker.infrastructure;

import net.fellbaum.jemoji.EmojiManager;

public class TextConstants {
    // TODO иконки в отдельный класс
    public static final String PERSONAGE_ICON = EmojiManager.getByAlias(":beginner:").orElseThrow().getEmoji();
    public static final String MONEY_ICON = EmojiManager.getByAlias(":moneybag:").orElseThrow().getEmoji();
    public static final String STRENGTH_ICON = EmojiManager.getByAlias(":fist:").orElseThrow().getEmoji();
    public static final String AGILITY_ICON = EmojiManager.getByAlias(":athletic_shoe:").orElseThrow().getEmoji();
    public static final String WISDOM_ICON = EmojiManager.getByAlias(":brain:").orElseThrow().getEmoji();
    public static final String ATTACK_ICON = EmojiManager.getByAlias(":crossed_swords:").orElseThrow().getEmoji();
    public static final String DEFENSE_ICON = EmojiManager.getByAlias(":shield:").orElseThrow().getEmoji();
    public static final String HEALTH_ICON = EmojiManager.getByAlias(":heart:").orElseThrow().getEmoji();
    public static final String ENERGY_ICON = EmojiManager.getByAlias(":battery:").orElseThrow().getEmoji();
    public static final String DEAD_ICON = EmojiManager.getByAlias(":dizzy_face:").orElseThrow().getEmoji();
    public static final String DUEL_WINNER_ICON = EmojiManager.getByAlias(":tada:").orElseThrow().getEmoji();
    public static final String DUEL_LOSER_ICON = DEAD_ICON;
    public static final String DEFAULT_NAME = "Безымянный";
    public static final String SOURCE_LINK = "https://github.com/Homyakin/project-seeker";
    public static final String TELEGRAM_CHANNEL_USERNAME = "@krezar_news";
    public static final String CALLBACK_DELIMITER = "~";
}
