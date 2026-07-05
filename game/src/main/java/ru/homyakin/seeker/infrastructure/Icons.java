package ru.homyakin.seeker.infrastructure;

import net.fellbaum.jemoji.EmojiManager;

import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;

public class Icons {
    public static final String MONEY = EmojiManager.getByAlias(":moneybag:").orElseThrow().getFirst().getEmoji();
    public static final String ATTACK = EmojiManager.getByAlias(":crossed_swords:").orElseThrow().getFirst().getEmoji();
    public static final String DEFENSE = EmojiManager.getByAlias(":shield:").orElseThrow().getFirst().getEmoji();
    public static final String HEALTH = EmojiManager.getByAlias(":heart:").orElseThrow().getFirst().getEmoji();
    public static final String ENERGY = EmojiManager.getByAlias(":battery:").orElseThrow().getFirst().getEmoji();
    public static final String POWER = EmojiManager.getByAlias(":muscle:").orElseThrow().getFirst().getEmoji();
    public static final String DEAD = EmojiManager.getByAlias(":dizzy_face:").orElseThrow().getFirst().getEmoji();
    public static final String DUEL_WINNER = EmojiManager.getByAlias(":tada:").orElseThrow().getFirst().getEmoji();
    public static final String DUEL_LOSER = DEAD;
    public static final String NORMAL_ATTACK = ATTACK;
    public static final String CRIT_ATTACK = EmojiManager.getByAlias(":boom:").orElseThrow().getFirst().getEmoji();
    public static final String CRIT_MULTIPLIER = EmojiManager.getByAlias(":comet:").orElseThrow().getFirst().getEmoji();
    public static final String BLOCKED_DAMAGE = DEFENSE;
    public static final String MISS = EmojiManager.getByAlias(":o:").orElseThrow().getFirst().getEmoji();
    public static final String DODGE = EmojiManager.getByAlias(":dash:").orElseThrow().getFirst().getEmoji();
    public static final String SPEED = EmojiManager.getByAlias(":zap:").orElseThrow().getFirst().getEmoji();
    public static final String RANGE = EmojiManager.getByAlias(":straight_ruler:").orElseThrow().getFirst().getEmoji();
    public static final String THREAT = EmojiManager.getByAlias(":dart:").orElseThrow().getFirst().getEmoji();
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

    // Attack types
    public static final String ATTACK_TYPE_SLASH = EmojiManager.getByAlias(":dagger:").orElseThrow().getFirst().getEmoji();
    public static final String ATTACK_TYPE_BLUNT = EmojiManager.getByAlias(":hammer:").orElseThrow().getFirst().getEmoji();
    public static final String ATTACK_TYPE_PIERCE = EmojiManager.getByAlias(":pushpin:").orElseThrow().getFirst().getEmoji();
    public static final String ATTACK_TYPE_MAGICAL = EmojiManager.getByAlias(":crystal_ball:").orElseThrow().getFirst().getEmoji();

    // Defense types
    public static final String DEFENSE_TYPE_CLOTH = EmojiManager.getByAlias(":kimono:").orElseThrow().getFirst().getEmoji();
    public static final String DEFENSE_TYPE_LEATHER = EmojiManager.getByAlias(":coat:").orElseThrow().getFirst().getEmoji();
    public static final String DEFENSE_TYPE_PLATE = EmojiManager.getByAlias(":chains:").orElseThrow().getFirst().getEmoji();
    public static final String DEFENSE_TYPE_ARCANE = EmojiManager.getByAlias(":sparkles:").orElseThrow().getFirst().getEmoji();

    public static String attackTypeIcon(AttackType attackType) {
        return switch (attackType) {
            case SLASH -> ATTACK_TYPE_SLASH;
            case BLUNT -> ATTACK_TYPE_BLUNT;
            case PIERCE -> ATTACK_TYPE_PIERCE;
            case MAGICAL -> ATTACK_TYPE_MAGICAL;
        };
    }

    public static String defenseTypeIcon(DefenseType defenseType) {
        return switch (defenseType) {
            case CLOTH -> DEFENSE_TYPE_CLOTH;
            case LEATHER -> DEFENSE_TYPE_LEATHER;
            case PLATE -> DEFENSE_TYPE_PLATE;
            case ARCANE -> DEFENSE_TYPE_ARCANE;
        };
    }

    // PersonageSlots
    public static final String MAIN_HAND = EmojiManager.getByAlias(":raised_hand:").orElseThrow().getFirst().getEmoji();
    public static final String OFF_HAND = EmojiManager.getByAlias(":palm_down_hand:").orElseThrow().getFirst().getEmoji();
    public static final String BODY = EmojiManager.getByAlias(":shirt:").orElseThrow().getFirst().getEmoji();
    public static final String SHOES = EmojiManager.getByAlias(":hiking_boot:").orElseThrow().getFirst().getEmoji();
    public static final String PANTS = EmojiManager.getByAlias(":jeans:").orElseThrow().getFirst().getEmoji();
    public static final String HELMET = EmojiManager.getByAlias(":billed_cap:").orElseThrow().getFirst().getEmoji();
    public static final String GLOVES = EmojiManager.getByAlias(":gloves:").orElseThrow().getFirst().getEmoji();

    // Outpost
    public static final String OUTPOST_MATERIALS = EmojiManager.getByAlias(":package:").orElseThrow().getFirst().getEmoji();

    // Contraband
    public static final String CONTRABAND_COMMON = EmojiManager.getByAlias(":package:").orElseThrow().getFirst().getEmoji();
    public static final String CONTRABAND_RARE = EmojiManager.getByAlias(":lock:").orElseThrow().getFirst().getEmoji();
    public static final String CONTRABAND_EPIC = EmojiManager.getByAlias(":eye:").orElseThrow().getFirst().getEmoji();
}
