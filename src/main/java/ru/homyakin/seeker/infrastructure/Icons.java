package ru.homyakin.seeker.infrastructure;

import net.fellbaum.jemoji.EmojiManager;

public class Icons {
    public static final String MONEY = EmojiManager.getByAlias(":moneybag:").orElseThrow().getFirst().getEmoji();
    public static final String STRENGTH = EmojiManager.getByAlias(":fist:").orElseThrow().getFirst().getEmoji();
    public static final String AGILITY = EmojiManager.getByAlias(":athletic_shoe:").orElseThrow().getFirst().getEmoji();
    public static final String WISDOM = EmojiManager.getByAlias(":brain:").orElseThrow().getFirst().getEmoji();
    public static final String ATTACK = EmojiManager.getByAlias(":crossed_swords:").orElseThrow().getFirst().getEmoji();
    public static final String DEFENSE = EmojiManager.getByAlias(":shield:").orElseThrow().getFirst().getEmoji();
    public static final String HEALTH = EmojiManager.getByAlias(":heart:").orElseThrow().getFirst().getEmoji();
    public static final String ENERGY = EmojiManager.getByAlias(":battery:").orElseThrow().getFirst().getEmoji();
    public static final String DEAD = EmojiManager.getByAlias(":dizzy_face:").orElseThrow().getFirst().getEmoji();
    public static final String DUEL_WINNER = EmojiManager.getByAlias(":tada:").orElseThrow().getFirst().getEmoji();
    public static final String DUEL_LOSER = DEAD;
    public static final String NORMAL_ATTACK = ATTACK;
    public static final String CRIT_ATTACK = EmojiManager.getByAlias(":boom:").orElseThrow().getFirst().getEmoji();
    public static final String BLOCKED_DAMAGE = DEFENSE;
    public static final String MISS = EmojiManager.getByAlias(":o:").orElseThrow().getFirst().getEmoji();
    public static final String DODGE = EmojiManager.getByAlias(":dash:").orElseThrow().getFirst().getEmoji();
    public static final String ENABLED = EmojiManager.getByAlias(":white_check_mark:").orElseThrow().getFirst().getEmoji();
    public static final String DISABLED = EmojiManager.getByAlias(":x:").orElseThrow().getFirst().getEmoji();
    public static final String RANDOM = EmojiManager.getByAlias(":game_die:").orElseThrow().getFirst().getEmoji();
    public static final String TIME = EmojiManager.getByAlias(":hourglass_flowing_sand:").orElseThrow().getFirst().getEmoji();
    public static final String EXHAUSTED = EmojiManager.getByAlias(":yawning_face:").orElseThrow().getFirst().getEmoji();
    public static final String STANDARD_GROUP_BADGE = EmojiManager.getByAlias(":beginner:").orElseThrow().getFirst().getEmoji();
    public static final String PARTICIPANTS = EmojiManager.getByAlias(":bust_in_silhouette:").orElseThrow().getFirst().getEmoji();
    public static final String SUCCESS_QUEST = EmojiManager.getByAlias(":white_check_mark:").orElseThrow().getFirst().getEmoji();
    public static final String FAILED_QUEST = EmojiManager.getByAlias(":x:").orElseThrow().getFirst().getEmoji();
    public static final String BROKEN_ITEM = EmojiManager.getByAlias(":broken_chain:").orElseThrow().getFirst().getEmoji();

    // PersonageSlots
    public static final String MAIN_HAND = EmojiManager.getByAlias(":raised_hand:").orElseThrow().getFirst().getEmoji();
    public static final String OFF_HAND = EmojiManager.getByAlias(":palm_down_hand:").orElseThrow().getFirst().getEmoji();
    public static final String BODY = EmojiManager.getByAlias(":shirt:").orElseThrow().getFirst().getEmoji();
    public static final String SHOES = EmojiManager.getByAlias(":hiking_boot:").orElseThrow().getFirst().getEmoji();
    public static final String PANTS = EmojiManager.getByAlias(":jeans:").orElseThrow().getFirst().getEmoji();
    public static final String HELMET = EmojiManager.getByAlias(":billed_cap:").orElseThrow().getFirst().getEmoji();
    public static final String GLOVES = EmojiManager.getByAlias(":gloves:").orElseThrow().getFirst().getEmoji();
}
