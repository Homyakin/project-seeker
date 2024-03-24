package ru.homyakin.seeker.infrastructure;

import net.fellbaum.jemoji.EmojiManager;

public class Icons {
    public static final String PERSONAGE_STANDARD = EmojiManager.getByAlias(":beginner:").orElseThrow().getEmoji();
    public static final String PERSONAGE_FIRST = EmojiManager.getByAlias(":reminder_ribbon:").orElseThrow().getEmoji();
    public static final String MONEY = EmojiManager.getByAlias(":moneybag:").orElseThrow().getEmoji();
    public static final String STRENGTH = EmojiManager.getByAlias(":fist:").orElseThrow().getEmoji();
    public static final String AGILITY = EmojiManager.getByAlias(":athletic_shoe:").orElseThrow().getEmoji();
    public static final String WISDOM = EmojiManager.getByAlias(":brain:").orElseThrow().getEmoji();
    public static final String ATTACK = EmojiManager.getByAlias(":crossed_swords:").orElseThrow().getEmoji();
    public static final String DEFENSE = EmojiManager.getByAlias(":shield:").orElseThrow().getEmoji();
    public static final String HEALTH = EmojiManager.getByAlias(":heart:").orElseThrow().getEmoji();
    public static final String ENERGY = EmojiManager.getByAlias(":battery:").orElseThrow().getEmoji();
    public static final String DEAD = EmojiManager.getByAlias(":dizzy_face:").orElseThrow().getEmoji();
    public static final String DUEL_WINNER = EmojiManager.getByAlias(":tada:").orElseThrow().getEmoji();
    public static final String DUEL_LOSER = DEAD;
    public static final String NORMAL_ATTACK = ATTACK;
    public static final String CRIT_ATTACK = EmojiManager.getByAlias(":boom:").orElseThrow().getEmoji();
    public static final String BLOCKED_DAMAGE = DEFENSE;
    public static final String MISS = EmojiManager.getByAlias(":o:").orElseThrow().getEmoji();
    public static final String DODGE = EmojiManager.getByAlias(":dash:").orElseThrow().getEmoji();

    // PersonageSlots
    public static final String MAIN_HAND = EmojiManager.getByAlias(":raised_hand:").orElseThrow().getEmoji();
    public static final String OFF_HAND = EmojiManager.getByAlias(":leftwards_hand:").orElseThrow().getEmoji();
}
