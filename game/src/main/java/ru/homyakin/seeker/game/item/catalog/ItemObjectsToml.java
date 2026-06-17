package ru.homyakin.seeker.game.item.catalog;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemObjectLocale;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record ItemObjectsToml(List<SavingItemObject> item) {
    private static final TomlMapper MAPPER = TomlMapper.builder()
        .addModule(new Jdk8Module())
        .build();

    public static ItemObjectsToml load(InputStream stream) {
        try {
            return MAPPER.readValue(stream, ItemObjectsToml.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load item objects catalog toml", e);
        }
    }

    public List<ItemObject> itemObjects() {
        return item.stream().map(SavingItemObject::toItemObject).toList();
    }

    public record SavingItemObject(
        String code,
        Set<PersonageSlot> slots,
        Optional<SavingItemAttack> attack,
        Optional<SavingItemDefense> defense,
        int health,
        int critChance,
        int dodgeChance,
        double critMultiplier,
        int speed,
        int baseThreat,
        Map<Language, ItemObjectLocale> locales
    ) implements Localized<ItemObjectLocale> {
        ItemObject toItemObject() {
            return new ItemObject(
                code,
                slots,
                attack.map(a -> new ItemAttack(a.attackType(), a.range(), a.attack())),
                defense.map(d -> new ItemDefense(d.defenseType(), d.defense())),
                health,
                critChance,
                dodgeChance,
                critMultiplier,
                speed,
                baseThreat,
                locales
            );
        }
    }

    public record SavingItemAttack(
        AttackType attackType,
        int range,
        int attack
    ) {
    }

    public record SavingItemDefense(
        DefenseType defenseType,
        int defense
    ) {
    }
}
