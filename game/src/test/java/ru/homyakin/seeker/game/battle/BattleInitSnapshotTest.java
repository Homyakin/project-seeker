package ru.homyakin.seeker.game.battle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class BattleInitSnapshotTest {
    @Test
    public void initSnapshotIncludesItemsSkillsAndCharacteristics() {
        final var items = List.of(
            catalogWeapon("sword", AttackType.SLASH, 1, 150, ActiveEnum.KNOCKBACK, ItemRarity.COMMON),
            catalogArmor("cuirass", DefenseType.PLATE, 200, 800, ActiveEnum.BLEEDING, ItemRarity.LEGENDARY),
            catalogArmor("greaves", DefenseType.PLATE, 50, 500, ActiveEnum.BLEEDING, ItemRarity.LEGENDARY)
        );
        final var personage = new BattlePersonage(
            items,
            Position.FRONT,
            Map.of(ActiveEnum.BLEEDING, 4)
        );
        final var result = new Battle().process(List.of(personage), List.of(
            new BattlePersonage(
                List.of(catalogWeapon("rapier", AttackType.PIERCE, 1, 100, ActiveEnum.FEINT, ItemRarity.COMMON)),
                Position.FRONT
            )
        ));

        final var snap = result.initState().personagesById().get(personage.id());
        Assertions.assertNotNull(snap);
        Assertions.assertEquals(3, snap.items().size());
        Assertions.assertEquals("sword", snap.items().getFirst().code());
        Assertions.assertEquals(ItemRarity.COMMON, snap.items().getFirst().rarity());
        Assertions.assertEquals(Optional.of(ActiveEnum.KNOCKBACK), snap.items().getFirst().skill());
        Assertions.assertEquals("cuirass", snap.items().get(1).code());
        Assertions.assertEquals(Optional.of(ActiveEnum.BLEEDING), snap.items().get(1).skill());
        Assertions.assertEquals(1, snap.skills().size());
        Assertions.assertEquals(ActiveEnum.BLEEDING, snap.skills().getFirst().code());
        Assertions.assertTrue(snap.skills().getFirst().points() > 0);
        Assertions.assertFalse(snap.attacksByRange().isEmpty());
        Assertions.assertEquals(150, snap.attacksByRange().getFirst().attack().get(AttackType.SLASH));
        Assertions.assertEquals(250, snap.defenses().get(DefenseType.PLATE));
        Assertions.assertTrue(snap.damageTakenMultipliers().containsKey(AttackType.SLASH));
        Assertions.assertTrue(snap.damageTakenMultipliers().get(AttackType.SLASH) > 0);
        Assertions.assertTrue(snap.damageTakenMultipliers().get(AttackType.SLASH) <= 1);
    }

    private static Item catalogWeapon(
        String code,
        AttackType attackType,
        int range,
        int attack,
        ActiveEnum skill,
        ItemRarity rarity
    ) {
        return new Item(
            new ItemObject(
                code,
                Set.of(PersonageSlot.MAIN_HAND),
                Optional.of(new ItemAttack(attackType, range, attack)),
                Optional.empty(),
                0,
                0,
                0,
                0,
                0,
                0,
                Map.of()
            ),
            Optional.of(new Modifier(skill)),
            rarity
        );
    }

    private static Item catalogArmor(
        String code,
        DefenseType defenseType,
        int defense,
        int health,
        ActiveEnum skill,
        ItemRarity rarity
    ) {
        return new Item(
            new ItemObject(
                code,
                Set.of(PersonageSlot.BODY),
                Optional.empty(),
                Optional.of(new ItemDefense(defenseType, defense)),
                health,
                0,
                0,
                0,
                0,
                0,
                Map.of()
            ),
            Optional.of(new Modifier(skill)),
            rarity
        );
    }
}
