package ru.homyakin.seeker.game.battle.v4;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import ru.homyakin.seeker.game.item.models.ItemObjectLocale;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.Language;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record ItemV4ObjectsToml(List<SavingV4ItemObject> item) {
    private static final TomlMapper MAPPER = TomlMapper.builder()
        .addModule(new Jdk8Module())
        .build();

    public static ItemV4ObjectsToml load(InputStream stream) {
        try {
            return MAPPER.readValue(stream, ItemV4ObjectsToml.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load item v4 objects toml", e);
        }
    }

    public List<ItemObject> itemObjects() {
        return item.stream().map(SavingV4ItemObject::toItemObject).toList();
    }

    public record SavingV4ItemObject(
        String code,
        Set<PersonageSlot> slots,
        Optional<SavingV4ItemAttack> attack,
        Optional<SavingV4ItemDefense> defense,
        int health,
        int critChance,
        int dodgeChance,
        double critMultiplier,
        int speed,
        int baseThreat,
        Map<Language, ItemObjectLocale> locales
    ) {
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

    public record SavingV4ItemAttack(
        AttackType attackType,
        int range,
        int attack
    ) {
    }

    public record SavingV4ItemDefense(
        DefenseType defenseType,
        int defense
    ) {
    }
}
