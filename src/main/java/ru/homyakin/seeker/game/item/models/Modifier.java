package ru.homyakin.seeker.game.item.models;

import ru.homyakin.seeker.game.battle.v4.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.modifier.models.ModifierLocale;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

import java.util.Map;
import java.util.Set;

public record Modifier(
    String code,
    ActiveEnum activeEnum,
    ModifierType type,
    Set<PersonageSlot> availableOnSlots,
    Map<Language, ModifierLocale> locales
) implements Localized<ModifierLocale> {

    public Modifier(ActiveEnum activeEnum) {
        this(
            activeEnum.name().toLowerCase(),
            activeEnum,
            ModifierType.ANY,
            Set.of(),
            Map.of()
        );
    }
}
