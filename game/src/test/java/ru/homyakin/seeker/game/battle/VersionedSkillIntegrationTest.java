package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.SkillRank;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidPersonage;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

class VersionedSkillIntegrationTest {
    @Test
    void defaultPersonageAndOldWorldRaidSkillStayOnLegacyFormulas() {
        final var defaultPersonage = new BattlePersonage(
            List.of(combatItem("default", List.of(attack(AttackType.SLASH, 10)), Optional.empty(), 100, 1_000)),
            Position.FRONT,
            Map.of(ActiveEnum.THORNS, 1)
        );
        final var oldWorldRaidPersonage = new BattlePersonage(
            new WorldRaidPersonage(
                List.of(new WorldRaidPersonage.PersonageSkill(ActiveEnum.SELF_HEAL, SkillRank.FIRST)),
                100,
                0,
                0,
                0,
                1_000,
                10,
                List.of(new WorldRaidPersonage.AttackTemplate(AttackType.BLUNT, 1, 10)),
                List.of(),
                Position.FRONT
            ),
            Position.FRONT
        );

        Assertions.assertAll(
            () -> Assertions.assertFalse(defaultPersonage.hasScalingSkills()),
            () -> Assertions.assertTrue(defaultPersonage.hasLegacySkill(ActiveEnum.THORNS)),
            () -> Assertions.assertEquals(
                SkillFormulaVersion.LEGACY_SKILLS_V1,
                formulaVersion(defaultPersonage, ActiveEnum.THORNS)
            ),
            () -> Assertions.assertFalse(oldWorldRaidPersonage.hasScalingSkills()),
            () -> Assertions.assertTrue(oldWorldRaidPersonage.hasLegacySkill(ActiveEnum.SELF_HEAL)),
            () -> Assertions.assertEquals(
                SkillFormulaVersion.LEGACY_SKILLS_V1,
                formulaVersion(oldWorldRaidPersonage, ActiveEnum.SELF_HEAL)
            )
        );

        final var context = new BattleContext(
            List.of(defaultPersonage),
            List.of(oldWorldRaidPersonage),
            new LowerBoundRandom()
        );
        Assertions.assertFalse(context.usesScalingSkillOrder());
    }

    @Test
    void explicitWorldRaidFormulaVersionReachesBattlePersonageAndItsSnapshot() {
        final var personage = new BattlePersonage(
            new WorldRaidPersonage(
                List.of(new WorldRaidPersonage.PersonageSkill(
                    ActiveEnum.TEMPO_BREAK,
                    SkillRank.THIRD,
                    SkillFormulaVersion.SCALING_SKILLS_V1
                )),
                100,
                0,
                0,
                0,
                1_000,
                10,
                List.of(new WorldRaidPersonage.AttackTemplate(AttackType.MAGICAL, 2, 10)),
                List.of(),
                Position.MID
            ),
            Position.MID
        );

        Assertions.assertAll(
            () -> Assertions.assertTrue(personage.hasScalingSkills()),
            () -> Assertions.assertTrue(personage.scalingSkills().has(ActiveEnum.TEMPO_BREAK)),
            () -> Assertions.assertFalse(personage.hasLegacySkill(ActiveEnum.TEMPO_BREAK)),
            () -> Assertions.assertEquals(
                SkillFormulaVersion.SCALING_SKILLS_V1,
                formulaVersion(personage, ActiveEnum.TEMPO_BREAK)
            )
        );
    }

    @Test
    void routesEverySkillByItsOwnVersionInsideOnePersonage() {
        final var personage = BattlePersonage.withSkillVersions(
            List.of(combatItem("mixed", List.of(attack(AttackType.SLASH, 10)), Optional.empty(), 100, 1_000)),
            Position.FRONT,
            Map.of(
                ActiveEnum.THORNS, 1,
                ActiveEnum.DOUBLE_ATTACK, 1
            ),
            Map.of(
                ActiveEnum.THORNS, SkillFormulaVersion.LEGACY_SKILLS_V1,
                ActiveEnum.DOUBLE_ATTACK, SkillFormulaVersion.SCALING_SKILLS_V1
            )
        );

        Assertions.assertAll(
            () -> Assertions.assertTrue(personage.hasLegacySkill(ActiveEnum.THORNS)),
            () -> Assertions.assertFalse(personage.scalingSkills().has(ActiveEnum.THORNS)),
            () -> Assertions.assertFalse(personage.hasLegacySkill(ActiveEnum.DOUBLE_ATTACK)),
            () -> Assertions.assertTrue(personage.scalingSkills().has(ActiveEnum.DOUBLE_ATTACK)),
            () -> Assertions.assertEquals(
                SkillFormulaVersion.LEGACY_SKILLS_V1,
                formulaVersion(personage, ActiveEnum.THORNS)
            ),
            () -> Assertions.assertEquals(
                SkillFormulaVersion.SCALING_SKILLS_V1,
                formulaVersion(personage, ActiveEnum.DOUBLE_ATTACK)
            )
        );
    }

    @Test
    void mixedBattleRunsLegacyReactionAndOneMixedScalingDamageEvent() {
        final var attacker = BattlePersonage.forScalingSkills(
            List.of(combatItem(
                "mixed-attacker",
                List.of(
                    attack(AttackType.SLASH, 7),
                    attack(AttackType.BLUNT, 7)
                ),
                Optional.empty(),
                100,
                1_000
            )),
            Position.FRONT,
            Map.of(ActiveEnum.DOUBLE_ATTACK, 1)
        );
        final var defender = new BattlePersonage(
            List.of(combatItem(
                "legacy-defender",
                List.of(attack(AttackType.PIERCE, 1)),
                Optional.of(new ItemDefense(DefenseType.CLOTH, 500)),
                100,
                1_000
            )),
            Position.FRONT,
            Map.of(ActiveEnum.THORNS, 1)
        );
        final var context = new BattleContext(
            List.of(attacker),
            List.of(defender),
            new LowerBoundRandom()
        );
        final var log = new BattleActionLog();

        Assertions.assertTrue(context.usesScalingSkillOrder());
        Assertions.assertFalse(VersionedBattleTurn.process(attacker, context, log, 1));

        final var legacyEvents = log.events().stream()
            .filter(BattleEvent.SkillDamage.class::isInstance)
            .map(BattleEvent.SkillDamage.class::cast)
            .filter(event -> event.skill() == ActiveEnum.THORNS)
            .toList();
        final var scalingEvents = log.events().stream()
            .filter(BattleEvent.ScalingSkillDamage.class::isInstance)
            .map(BattleEvent.ScalingSkillDamage.class::cast)
            .filter(event -> event.skill() == ActiveEnum.DOUBLE_ATTACK)
            .toList();

        Assertions.assertEquals(1, legacyEvents.size());
        Assertions.assertEquals(1, scalingEvents.size());
        final var scalingDamage = scalingEvents.getFirst();
        Assertions.assertAll(
            () -> Assertions.assertEquals(
                Map.of(AttackType.SLASH, 7, AttackType.BLUNT, 7),
                scalingDamage.basis()
            ),
            () -> Assertions.assertEquals(8, scalingDamage.coefficientNumerator()),
            () -> Assertions.assertEquals(50, scalingDamage.coefficientDenominator()),
            () -> Assertions.assertFalse(scalingDamage.periodic()),
            () -> Assertions.assertEquals(1, scalingDamage.damageTaken())
        );
    }

    @Test
    void battleInitSnapshotPreservesTheFormulaVersionOfEverySkill() {
        final var scaling = BattlePersonage.forScalingSkills(
            List.of(combatItem("scaling", List.of(attack(AttackType.SLASH, 200)), Optional.empty(), 100, 1_000)),
            Position.FRONT,
            Map.of(ActiveEnum.DOUBLE_ATTACK, 4)
        );
        final var legacy = new BattlePersonage(
            List.of(combatItem("legacy", List.of(attack(AttackType.BLUNT, 1)), Optional.empty(), 100, 0)),
            Position.FRONT,
            Map.of(ActiveEnum.SELF_HEAL, 4)
        );

        final var result = new Battle(new LowerBoundRandom()).process(
            List.of(scaling),
            List.of(legacy),
            3
        );

        Assertions.assertAll(
            () -> Assertions.assertEquals(
                SkillFormulaVersion.SCALING_SKILLS_V1,
                formulaVersion(result.initState(), scaling, ActiveEnum.DOUBLE_ATTACK)
            ),
            () -> Assertions.assertEquals(
                SkillFormulaVersion.LEGACY_SKILLS_V1,
                formulaVersion(result.initState(), legacy, ActiveEnum.SELF_HEAL)
            )
        );
    }

    @Test
    void primaryAttackTypeUsesStableEnumOrderWhenTotalsAreEqual() {
        final var allTypesTied = new BattlePersonage(
            List.of(combatItem(
                "all-types-tied",
                List.of(
                    attack(AttackType.MAGICAL, 10),
                    attack(AttackType.PIERCE, 10),
                    attack(AttackType.BLUNT, 10),
                    attack(AttackType.SLASH, 4),
                    attack(AttackType.SLASH, 6)
                ),
                Optional.empty(),
                100,
                1_000
            )),
            Position.FRONT
        );
        final var noSlashTie = new BattlePersonage(
            List.of(combatItem(
                "no-slash-tie",
                List.of(
                    attack(AttackType.MAGICAL, 10),
                    attack(AttackType.PIERCE, 10),
                    attack(AttackType.BLUNT, 10)
                ),
                Optional.empty(),
                100,
                1_000
            )),
            Position.FRONT
        );

        Assertions.assertEquals(Optional.of(AttackType.SLASH), allTypesTied.primaryAttackType());
        Assertions.assertEquals(Optional.of(AttackType.BLUNT), noSlashTie.primaryAttackType());
    }

    private static SkillFormulaVersion formulaVersion(BattlePersonage personage, ActiveEnum skill) {
        return personage.skillSnapshots().stream()
            .filter(snapshot -> snapshot.code() == skill)
            .findFirst()
            .orElseThrow()
            .formulaVersion();
    }

    private static SkillFormulaVersion formulaVersion(
        BattleInitState state,
        BattlePersonage personage,
        ActiveEnum skill
    ) {
        return state.personagesById().get(personage.id()).skills().stream()
            .filter(snapshot -> snapshot.code() == skill)
            .findFirst()
            .orElseThrow()
            .formulaVersion();
    }

    private static ItemAttack attack(AttackType type, int amount) {
        return new ItemAttack(type, 1, 1, amount);
    }

    private static Item combatItem(
        String code,
        List<ItemAttack> attacks,
        Optional<ItemDefense> defense,
        int health,
        int speed
    ) {
        return new Item(
            new ItemObject(
                code,
                Set.of(PersonageSlot.MAIN_HAND),
                attacks,
                defense,
                health,
                0,
                0,
                0,
                speed,
                10,
                0,
                ItemProgressionVersion.LEGACY,
                Map.of()
            ),
            Optional.empty(),
            ItemRarity.COMMON
        );
    }

    private static final class LowerBoundRandom implements BattleRandom {
        @Override
        public int nextInt(String sequence, int minimumInclusive, int maximumInclusive) {
            if (sequence.startsWith("skill-damage-")) {
                Assertions.assertTrue(minimumInclusive <= 0);
                Assertions.assertTrue(maximumInclusive >= 0);
                return 0;
            }
            return minimumInclusive;
        }
    }
}
