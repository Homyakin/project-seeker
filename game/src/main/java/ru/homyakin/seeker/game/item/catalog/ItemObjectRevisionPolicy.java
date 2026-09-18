package ru.homyakin.seeker.game.item.catalog;

import java.util.Objects;
import ru.homyakin.seeker.game.item.models.ItemObject;

/** Defines which catalog changes invalidate an already shown storm enhancement action. */
public final class ItemObjectRevisionPolicy {
    private ItemObjectRevisionPolicy() {
    }

    public static boolean invalidatesEnhancement(ItemObject previous, ItemObject replacement) {
        Objects.requireNonNull(previous, "Previous item object must be specified");
        Objects.requireNonNull(replacement, "Replacement item object must be specified");
        if (!Objects.equals(previous.code(), replacement.code())) {
            throw new IllegalArgumentException("Item object codes must match");
        }
        return !previous.slots().equals(replacement.slots())
            || !previous.attacks().equals(replacement.attacks())
            || !previous.defense().equals(replacement.defense())
            || previous.health() != replacement.health()
            || previous.critChance() != replacement.critChance()
            || previous.dodgeChance() != replacement.dodgeChance()
            || Double.compare(previous.critMultiplier(), replacement.critMultiplier()) != 0
            || previous.speed() != replacement.speed()
            || previous.baseThreat() != replacement.baseThreat()
            || previous.impact() != replacement.impact()
            || previous.progressionVersion() != replacement.progressionVersion();
    }
}
