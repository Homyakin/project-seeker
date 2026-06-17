package ru.homyakin.seeker.game.item.models;

import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.WordForm;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class DefaultItems {
    public static final Item MAIN_FIST = new Item(
        new ItemObject(
            "main-fists",
            Set.of(PersonageSlot.MAIN_HAND),
            Optional.of(new ItemAttack(
                AttackType.BLUNT,
                1,
                150
            )),
            Optional.empty(),
            0,
            1,
            1,
            0.1,
            15,
            3,
            Map.of(
                Language.RU, new ItemObjectLocale("Кулак", WordForm.PLURAL)
            )
        ),
        Optional.empty(),
        ItemRarity.COMMON
    );
    public static final Item OFF_FIST = new Item(
        new ItemObject(
            "off-fists",
            Set.of(PersonageSlot.OFF_HAND),
            Optional.of(new ItemAttack(
                AttackType.BLUNT,
                1,
                50
            )),
            Optional.empty(),
            0,
            1,
            1,
            0.1,
            15,
            3,
            Map.of(
                Language.RU, new ItemObjectLocale("Кулак", WordForm.PLURAL)
            )
        ),
        Optional.empty(),
        ItemRarity.COMMON
    );

    public static final Item PANTS = new Item(
        new ItemObject(
            "leaky_pants",
            Set.of(PersonageSlot.PANTS),
            Optional.empty(),
            Optional.of(
                new ItemDefense(
                    DefenseType.CLOTH,
                    24
                )
            ),
            180,
            2,
            2,
            0.1,
            30,
            5,
            Map.of(
                Language.RU, new ItemObjectLocale("Дырявые Штаны", WordForm.PLURAL)
            )
        ),
        Optional.empty(),
        ItemRarity.COMMON
    );

    public static final Item SHIRT = new Item(
        new ItemObject(
            "torn_jacket",
            Set.of(PersonageSlot.BODY),
            Optional.empty(),
            Optional.of(
                new ItemDefense(
                    DefenseType.CLOTH,
                    30
                )
            ),
            300,
            2,
            7,
            0.1,
            30,
            5,
            Map.of(
                Language.RU, new ItemObjectLocale("Рваная Кофта", WordForm.FEMININE)
            )
        ),
        Optional.empty(),
        ItemRarity.COMMON
    );

    public static final Item SHOES = new Item(
        new ItemObject(
            "footcloths",
            Set.of(PersonageSlot.SHOES),
            Optional.empty(),
            Optional.of(
                new ItemDefense(
                    DefenseType.CLOTH,
                    4
                )
            ),
            80,
            1,
            1,
            0.05,
            20,
            1,
            Map.of(
                Language.RU, new ItemObjectLocale("Портянки", WordForm.PLURAL)
            )
        ),
        Optional.empty(),
        ItemRarity.COMMON
    );

    public static final Item HELMET = new Item(
        new ItemObject(
            "panama",
            Set.of(PersonageSlot.HELMET),
            Optional.empty(),
            Optional.of(
                new ItemDefense(
                    DefenseType.CLOTH,
                    3
                )
            ),
            80,
            1,
            3,
            0.05,
            15,
            1,
            Map.of(
                Language.RU, new ItemObjectLocale("Панамка", WordForm.FEMININE)
            )
        ),
        Optional.empty(),
        ItemRarity.COMMON
    );

    public static final Item GLOVES = new Item(
        new ItemObject(
            "bandages",
            Set.of(PersonageSlot.GLOVES),
            Optional.empty(),
            Optional.of(
                new ItemDefense(
                    DefenseType.CLOTH,
                    3
                )
            ),
            80,
            1,
            3,
            0.05,
            15,
            1,
            Map.of(
                Language.RU, new ItemObjectLocale("Бинты", WordForm.PLURAL)
            )
        ),
        Optional.empty(),
        ItemRarity.COMMON
    );

    public static Optional<Item> defaultItemForSlot(PersonageSlot slot, Set<PersonageSlot> occupiedSlots) {
        return switch (slot) {
            case MAIN_HAND -> occupiedSlots.contains(PersonageSlot.MAIN_HAND)
                ? Optional.empty()
                : Optional.of(MAIN_FIST);
            case OFF_HAND -> occupiedSlots.contains(PersonageSlot.OFF_HAND)
                ? Optional.empty()
                : Optional.of(OFF_FIST);
            case BODY -> occupiedSlots.contains(PersonageSlot.BODY) ? Optional.empty() : Optional.of(SHIRT);
            case PANTS -> occupiedSlots.contains(PersonageSlot.PANTS) ? Optional.empty() : Optional.of(PANTS);
            case SHOES -> occupiedSlots.contains(PersonageSlot.SHOES) ? Optional.empty() : Optional.of(SHOES);
            case HELMET -> occupiedSlots.contains(PersonageSlot.HELMET) ? Optional.empty() : Optional.of(HELMET);
            case GLOVES -> occupiedSlots.contains(PersonageSlot.GLOVES) ? Optional.empty() : Optional.of(GLOVES);
        };
    }

    public static Characteristics characteristicsForFreeSlots(Set<PersonageSlot> occupiedSlots) {
        final var result = Characteristics.empty();
        if (!occupiedSlots.contains(PersonageSlot.MAIN_HAND)) {
            result.add(MAIN_FIST.visibleCharacteristics());
        }
        if (!occupiedSlots.contains(PersonageSlot.OFF_HAND)) {
            result.add(OFF_FIST.visibleCharacteristics());
        }
        if (!occupiedSlots.contains(PersonageSlot.BODY)) {
            result.add(SHIRT.visibleCharacteristics());
        }
        if (!occupiedSlots.contains(PersonageSlot.PANTS)) {
            result.add(PANTS.visibleCharacteristics());
        }
        if (!occupiedSlots.contains(PersonageSlot.SHOES)) {
            result.add(SHOES.visibleCharacteristics());
        }
        if (!occupiedSlots.contains(PersonageSlot.HELMET)) {
            result.add(HELMET.visibleCharacteristics());
        }
        if (!occupiedSlots.contains(PersonageSlot.GLOVES)) {
            result.add(GLOVES.visibleCharacteristics());
        }
        return result;
    }
}
