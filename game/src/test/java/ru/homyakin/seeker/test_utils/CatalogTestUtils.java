package ru.homyakin.seeker.test_utils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.CatalogItemObject;
import ru.homyakin.seeker.game.item.models.CatalogModifier;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemObjectLocale;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.WordForm;

public final class CatalogTestUtils {
    private CatalogTestUtils() {
    }

    public static CatalogItemObject catalogObject(int id, PersonageSlot... slots) {
        return new CatalogItemObject(id, itemObject(slots));
    }

    public static CatalogModifier catalogModifier(int id) {
        return new CatalogModifier(id, new Modifier(ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum.COUNTER_ATTACK));
    }

    public static ItemObject itemObject(PersonageSlot... slots) {
        return new ItemObject(
            "test-object-" + TestRandom.nextLong(),
            Set.of(slots),
            Optional.of(new ItemAttack(AttackType.SLASH, 1, 300)),
            Optional.empty(),
            0,
            0,
            0,
            0,
            15,
            0,
            Map.of(Language.RU, new ItemObjectLocale("Test Item", WordForm.MASCULINE))
        );
    }
}
