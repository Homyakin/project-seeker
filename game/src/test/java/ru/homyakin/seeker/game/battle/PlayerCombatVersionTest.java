package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.game.personage.power.PowerDisplaySnapshot;

class PlayerCombatVersionTest {
    @Test
    void newPlayerCombatRulesAreExplicitAndDisabledByDefault() {
        final var config = new PlayerCombatConfig();
        Assertions.assertEquals(SkillFormulaVersion.LEGACY_SKILLS_V1, config.version());
        config.setVersion(SkillFormulaVersion.SCALING_SKILLS_V2);
        Assertions.assertEquals(SkillFormulaVersion.SCALING_SKILLS_V2, config.version());
        Assertions.assertThrows(IllegalArgumentException.class, () -> config.setVersion(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> BattlePersonage.forCombat(
            ordinaryItems(), Position.FRONT, PersonageEffects.EMPTY, Optional.empty(), null
        ));
    }

    @ParameterizedTest
    @EnumSource(value = SkillFormulaVersion.class, names = {"SCALING_SKILLS_V2", "SCALING_SKILLS_V3"})
    void explicitModernVersionUsesOrderedTurnsEvenWithoutAnySkill(SkillFormulaVersion version) {
        final var commonItems = ordinaryItems();
        final var player = BattlePersonage.forCombat(
            commonItems, Position.FRONT, PersonageEffects.EMPTY, Optional.of("player"),
            version
        );
        final var legacyEnemy = new BattlePersonage(commonItems, Position.FRONT);
        final var context = new BattleContext(List.of(player), List.of(legacyEnemy));
        final var log = new BattleActionLog();

        player.move(context, log, 1);

        Assertions.assertAll(
            () -> Assertions.assertTrue(player.usesScalingCombatRules()),
            () -> Assertions.assertTrue(player.skillSnapshots().isEmpty()),
            () -> Assertions.assertTrue(context.usesScalingSkillOrder()),
            () -> Assertions.assertTrue(log.traceEvents().stream()
                .anyMatch(BattleTraceEvent.NormalAttackAttempt.class::isInstance)),
            () -> Assertions.assertEquals(Optional.of("player"), player.name())
        );
    }

    @ParameterizedTest
    @EnumSource(value = SkillFormulaVersion.class, names = {"SCALING_SKILLS_V2", "SCALING_SKILLS_V3"})
    void explicitModernVersionAssignsTheSelectedVersionToAllEquippedSkills(SkillFormulaVersion version) {
        final var catalog = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V3);
        final var sword = catalog.itemObjects().stream().filter(object -> object.code().equals("sword"))
            .findFirst().orElseThrow();
        final var equipped = new Item(sword, Optional.of(new Modifier(ActiveEnum.DOUBLE_ATTACK)), ItemRarity.RARE);
        final var player = BattlePersonage.forCombat(
            List.of(equipped), Position.FRONT, PersonageEffects.EMPTY, Optional.empty(),
            version
        );

        Assertions.assertEquals(
            List.of(new BattleSkillInitSnapshot(ActiveEnum.DOUBLE_ATTACK, 2, version)),
            player.skillSnapshots()
        );
    }

    @Test
    void oldFactoryAndConstructorsDoNotSilentlySwitchRulesOrInventDisplaySnapshots() {
        final var oldFactory = BattlePersonage.forCombat(
            ordinaryItems(), Position.FRONT, PersonageEffects.EMPTY, Optional.empty()
        );
        final var constructor = new BattlePersonage(ordinaryItems(), Position.FRONT);
        final var explicitLegacy = BattlePersonage.forCombat(
            ordinaryItems(), Position.FRONT, PersonageEffects.EMPTY, Optional.empty(),
            SkillFormulaVersion.LEGACY_SKILLS_V1
        );

        for (final var personage : List.of(oldFactory, constructor, explicitLegacy)) {
            Assertions.assertFalse(personage.usesScalingCombatRules());
            Assertions.assertTrue(personage.battlePersonageStats().displayedPower().isEmpty());
        }
    }

    @ParameterizedTest
    @EnumSource(value = SkillFormulaVersion.class, names = {"SCALING_SKILLS_V2", "SCALING_SKILLS_V3"})
    void referencePowerSnapshotUsesOriginalEquipmentNotTemporaryEffectsOrDamage(SkillFormulaVersion version) {
        final var catalog = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V3);
        final var items = catalog.itemObjects().stream()
            .filter(object -> List.of(
                "sword", "shield", "breastplate", "leather_chausses", "boots", "leather_helm", "leather_gloves"
            ).contains(object.code()))
            .map(Item::fromObject)
            .toList();
        final var effects = new PersonageEffects(Map.of(
            PersonageEffectType.WORKER_OF_DAY_EFFECT,
            new PersonageEffect(new Effect.Multiplier(50, EffectCharacteristic.HEALTH), null)
        ));
        final var player = BattlePersonage.forCombat(
            items, Position.FRONT, effects, Optional.empty(), version
        );
        final var originalSnapshot = PowerDisplaySnapshot.forEquipment(items);
        final var before = player.battlePersonageStats();
        player.applyVersionedSkillDamage(10, 10, player);

        Assertions.assertAll(
            () -> Assertions.assertTrue(originalSnapshot.isPresent()),
            () -> Assertions.assertEquals(originalSnapshot, before.displayedPower()),
            () -> Assertions.assertEquals(originalSnapshot, player.battlePersonageStats().displayedPower()),
            () -> Assertions.assertEquals(1000, originalSnapshot.orElseThrow().value())
        );
    }

    @Test
    void explicitThirdCombatFactoryPreservesItsVersionAndDoesNotUpgradeTheSecondFactory() {
        final var skills = Map.of(ActiveEnum.PENETRATION, 2);
        final var second = BattlePersonage.forScalingSkillsV2Combat(
            ordinaryItems(), Position.FRONT, skills, Optional.empty()
        );
        final var third = BattlePersonage.forScalingSkillsV3Combat(
            ordinaryItems(), Position.FRONT, skills, Optional.of("third")
        );
        final var emptyThird = BattlePersonage.forScalingSkillsV3Combat(
            ordinaryItems(), Position.FRONT, Map.of(), Optional.empty()
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(SkillFormulaVersion.SCALING_SKILLS_V2,
                second.scalingSkills().version(ActiveEnum.PENETRATION)),
            () -> Assertions.assertEquals(SkillFormulaVersion.SCALING_SKILLS_V3,
                third.scalingSkills().version(ActiveEnum.PENETRATION)),
            () -> Assertions.assertTrue(third.usesScalingCombatRules()),
            () -> Assertions.assertTrue(emptyThird.usesScalingCombatRules()),
            () -> Assertions.assertTrue(emptyThird.skillSnapshots().isEmpty()),
            () -> Assertions.assertEquals(Optional.of("third"), third.name())
        );
    }

    @ParameterizedTest
    @EnumSource(value = SkillFormulaVersion.class, names = {"SCALING_SKILLS_V1", "SCALING_SKILLS_V2", "SCALING_SKILLS_V3"})
    void explicitScalingFactoryKeepsTheRequestedFormula(SkillFormulaVersion version) {
        final var personage = BattlePersonage.forScalingSkills(
            ordinaryItems(), Position.FRONT, Map.of(ActiveEnum.HIT_AND_RUN, 2), version
        );

        Assertions.assertEquals(version, personage.scalingSkills().version(ActiveEnum.HIT_AND_RUN));
        Assertions.assertEquals(version, personage.skillSnapshots().getFirst().formulaVersion());
    }

    @Test
    void loadingExplicitPerSkillVersionsDoesNotUpgradeHistoricalSkills() {
        final var versions = Map.of(
            ActiveEnum.BERSERK, SkillFormulaVersion.SCALING_SKILLS_V1,
            ActiveEnum.DOUBLE_ATTACK, SkillFormulaVersion.SCALING_SKILLS_V2,
            ActiveEnum.PENETRATION, SkillFormulaVersion.SCALING_SKILLS_V3
        );
        final var personage = BattlePersonage.withSkillVersions(
            ordinaryItems(), Position.FRONT,
            Map.of(ActiveEnum.BERSERK, 2, ActiveEnum.DOUBLE_ATTACK, 2, ActiveEnum.PENETRATION, 2), versions
        );

        Assertions.assertEquals(3, personage.skillSnapshots().size());
        for (final var snapshot : personage.skillSnapshots()) {
            Assertions.assertEquals(versions.get(snapshot.code()), snapshot.formulaVersion());
            Assertions.assertEquals(versions.get(snapshot.code()), personage.scalingSkills().version(snapshot.code()));
        }
    }

    private static List<Item> ordinaryItems() {
        return List.of(
            Item.weapon(AttackType.SLASH, 1, 60, new Modifier(ActiveEnum.DOUBLE_ATTACK), ItemRarity.COMMON),
            Item.armor(DefenseType.PLATE, 0, 600, new Modifier(ActiveEnum.SELF_HEAL), ItemRarity.COMMON)
        );
    }
}
