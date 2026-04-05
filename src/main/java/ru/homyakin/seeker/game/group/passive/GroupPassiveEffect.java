package ru.homyakin.seeker.game.group.passive;

/**
 * A passive bonus tied to the group (outpost buildings, etc.). Domain data only — copy is built in
 * {@link ru.homyakin.seeker.locale.common.CommonLocalization}. Not the same model as personage buffs
 * ({@link ru.homyakin.seeker.game.personage.models.effect.PersonageEffects}).
 */
public sealed interface GroupPassiveEffect permits GroupBuildingPassiveEffect {
}
