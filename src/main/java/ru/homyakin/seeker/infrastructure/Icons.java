package ru.homyakin.seeker.infrastructure;

import net.fellbaum.jemoji.EmojiManager;

public class Icons {
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
    public static final String ENABLED = EmojiManager.getByAlias(":white_check_mark:").orElseThrow().getEmoji();
    public static final String DISABLED = EmojiManager.getByAlias(":x:").orElseThrow().getEmoji();
    public static final String RANDOM = EmojiManager.getByAlias(":game_die:").orElseThrow().getEmoji();
    public static final String TIME = EmojiManager.getByAlias(":hourglass_flowing_sand:").orElseThrow().getEmoji();
    public static final String EXHAUSTED = EmojiManager.getByAlias(":yawning_face:").orElseThrow().getEmoji();
    public static final String STANDARD_GROUP_BADGE = EmojiManager.getByAlias(":beginner:").orElseThrow().getEmoji();
    public static final String PARTICIPANTS = EmojiManager.getByAlias(":bust_in_silhouette:").orElseThrow().getEmoji();

    // PersonageSlots
    public static final String MAIN_HAND = EmojiManager.getByAlias(":raised_hand:").orElseThrow().getEmoji();
    public static final String OFF_HAND = EmojiManager.getByAlias(":palm_down_hand:").orElseThrow().getEmoji();
    public static final String BODY = EmojiManager.getByAlias(":shirt:").orElseThrow().getEmoji();
    public static final String SHOES = EmojiManager.getByAlias(":hiking_boot:").orElseThrow().getEmoji();
    public static final String PANTS = EmojiManager.getByAlias(":jeans:").orElseThrow().getEmoji();
    public static final String HELMET = EmojiManager.getByAlias(":billed_cap:").orElseThrow().getEmoji();
    public static final String GLOVES = EmojiManager.getByAlias(":gloves:").orElseThrow().getEmoji();
}
