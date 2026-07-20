package ru.homyakin.seeker.game.battle;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;

import java.util.List;

public class BattleInitSnapshotTest {
    @Test
    public void initSnapshotIncludesItemsSkillsAndCharacteristics() {
        final var items = List.of(
            Item.weapon(AttackType.SLASH, 1, 150, new Modifier(ActiveEnum.KNOCKBACK), ItemRarity.COMMON),
            Item.armor(DefenseType.PLATE, 200, 800, new Modifier(ActiveEnum.BLEEDING), ItemRarity.LEGENDARY),
            Item.armor(DefenseType.PLATE, 50, 500, new Modifier(ActiveEnum.BLEEDING), ItemRarity.LEGENDARY)
        );
        final var personage = new BattlePersonage(
            items,
            Position.FRONT,
            java.util.Map.of(ActiveEnum.BLEEDING, 4)
        );
        final var result = new Battle().process(List.of(personage), List.of(
            new BattlePersonage(
                List.of(Item.weapon(AttackType.PIERCE, 1, 100, new Modifier(ActiveEnum.FEINT), ItemRarity.COMMON)),
                Position.FRONT
            )
        ));

        final var snap = result.initState().personagesById().get(personage.id());
        Assertions.assertNotNull(snap);
        Assertions.assertFalse(snap.items().isEmpty());
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
}
