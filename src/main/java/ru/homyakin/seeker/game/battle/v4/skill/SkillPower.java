package ru.homyakin.seeker.game.battle.v4.skill;

/**
 * Shared tuning for {@link ItemSkill#skillPowerRating(SkillPowerInputs)} when a skill depends on exchange rates
 * not stored on the skill itself.
 */
public final class SkillPower {
    /**
     * Roughly how often you take a non-dodged hit per own successful strike (for reactive damage).
     */
    public static final double RECEIVED_DAMAGE_EVENTS_PER_OWN_ATTACK = 0.55;

    private SkillPower() {
    }
}
