package ru.homyakin.seeker.game.battle;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.ReferenceTeam;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Specialization;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Tier;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.storm.ItemProgression;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.RandomUtils;

class Step123ReferenceTeamsTest {
    @Test
    void mainTeamsCoverEverySizeWithLegalCatalogItemsAndExplicitCombatOrder() {
        for (final var tier : Tier.values()) {
            for (int size = 1; size <= 30; size++) {
                final var team = Step123ReferenceTeams.mainTeam(tier, size);
                Assertions.assertEquals(size, team.members().size());
                Assertions.assertEquals(team, Step123ReferenceTeams.mainTeam(tier, size));
                Assertions.assertEquals(size == 1 ? 0 : 1,
                    codes(team).stream().filter("GUARDIAN"::equals).count());
                assertLegal(team);
                final var personages = RandomUtils.withSeed(2026102306L, team::personages);
                Assertions.assertEquals(size, personages.size());
                final var context = new BattleContext(personages, team.personages(), new SeededBattleRandom(2026102306L));
                Assertions.assertTrue(context.usesScalingSkillOrder());
                for (final var personage : personages) {
                    Assertions.assertTrue(personage.skillSnapshots().stream().allMatch(snapshot ->
                        snapshot.formulaVersion() == SkillFormulaVersion.SCALING_SKILLS_V2
                    ));
                }
            }
        }
        Assertions.assertThrows(IllegalArgumentException.class, () -> Step123ReferenceTeams.mainTeam(Tier.MID, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Step123ReferenceTeams.mainTeam(Tier.MID, 31));
    }

    @Test
    void mainCompositionUsesSoloBruiserOrOneGuardianAndTheRangerMageBruiserCycle() {
        Assertions.assertEquals(List.of("BRUISER"), codes(Step123ReferenceTeams.mainTeam(Tier.MID, 1)));
        final var three = Step123ReferenceTeams.mainTeam(Tier.MID, 3);
        Assertions.assertEquals(List.of("GUARDIAN", "RANGER_PHYSICAL", "RANGER_MAGICAL"), codes(three));
        Assertions.assertEquals(List.of(Position.FRONT, Position.MID, Position.MID), three.members().stream()
            .map(member -> member.build().position()).toList());
        final var seven = Step123ReferenceTeams.mainTeam(Tier.MID, 7);
        Assertions.assertEquals(List.of("GUARDIAN", "RANGER_PHYSICAL", "RANGER_MAGICAL", "BRUISER",
            "RANGER_PHYSICAL", "RANGER_MAGICAL", "BRUISER"), codes(seven));
        final var fifteen = Step123ReferenceTeams.mainTeam(Tier.MID, 15);
        Assertions.assertEquals(Map.of("GUARDIAN", 1L, "BRUISER", 4L, "RANGER_PHYSICAL", 5L, "RANGER_MAGICAL", 5L),
            codes(fifteen).stream().collect(java.util.stream.Collectors.groupingBy(
                java.util.function.Function.identity(), java.util.stream.Collectors.counting())));
        for (final var member : seven.members()) {
            final var build = member.build();
            final String carrier = switch (build.code()) {
                case "GUARDIAN" -> "tower_shield";
                case "BRUISER" -> "sledgehammer";
                case "RANGER_PHYSICAL" -> "longbow";
                case "RANGER_MAGICAL" -> "battle_staff";
                default -> throw new IllegalStateException(build.code());
            };
            Assertions.assertEquals(Set.of(carrier), build.assignments().keySet());
            Assertions.assertEquals(build.code().equals("GUARDIAN") ? ItemRarity.RARE : ItemRarity.UNCOMMON,
                build.assignments().get(carrier).rarity());
            Assertions.assertEquals(build.code().startsWith("RANGER") ? Position.MID : Position.FRONT, build.position());
        }
    }

    @Test
    void workingInputsUseZeroThreeSixWhileTenAndTwentyRemainTechnicalControls() {
        Assertions.assertEquals(List.of(0, 3, 6, 10, 20), java.util.Arrays.stream(Tier.values()).map(Tier::level).toList());
        Assertions.assertEquals(List.of(0, 2, 4, 4, 4), java.util.Arrays.stream(Tier.values()).map(Tier::profilePoints).toList());
        final var beginner = Step123ReferenceTeams.mainTeam(Tier.BEGINNER, 15);
        for (final var tier : Tier.values()) {
            Assertions.assertEquals(tier == Tier.EXTENDED || tier == Tier.STRESS ? "TECHNICAL_CONTROL" : "WORKING_REFERENCE",
                tier.group());
            final var team = Step123ReferenceTeams.mainTeam(tier, 15);
            for (int index = 0; index < team.members().size(); index++) {
                final var member = team.members().get(index);
                Assertions.assertEquals(beginner.members().get(index).build().itemCodes(), member.build().itemCodes());
                Assertions.assertEquals(tier.profilePoints(), member.items(tier.level()).stream()
                    .mapToInt(item -> item.skillPoints()).sum());
                if (tier == Tier.BEGINNER) {
                    Assertions.assertTrue(member.items(0).stream().allMatch(item ->
                        item.rarity() == ItemRarity.COMMON && item.modifier().isEmpty() && item.enhanceLevel() == 0
                    ));
                }
            }
        }
    }

    @Test
    void mobileUsesOneSpearDirkFrontAndTwoCommonCrossbowSupportsInMid() {
        for (final var tier : Tier.values()) {
            final var mobile = Step123ReferenceTeams.specializedTeam(tier, Specialization.MOBILE);
            final var tested = mobile.members().getFirst().build();
            Assertions.assertEquals(List.of("spear", "dirk", "breastplate", "leather_chausses", "boots",
                "leather_helm", "leather_gloves"), tested.itemCodes());
            Assertions.assertEquals(Position.FRONT, tested.position());
            final Set<String> expectedCarriers = switch (tier.profilePoints()) {
                case 0 -> Set.of();
                case 2 -> Set.of("boots");
                case 4 -> Set.of("boots", "leather_helm");
                default -> throw new IllegalStateException();
            };
            Assertions.assertEquals(expectedCarriers, tested.assignments().keySet());
            tested.assignments().values().forEach(assignment -> {
                Assertions.assertEquals(ActiveEnum.HIT_AND_RUN, assignment.skill());
                Assertions.assertEquals(ItemRarity.RARE, assignment.rarity());
            });
            Assertions.assertEquals(tier.profilePoints(), mobile.members().getFirst().items(tier.level()).stream()
                .mapToInt(item -> item.skillPoints()).sum());
            for (final var member : mobile.members().subList(1, 3)) {
                Assertions.assertEquals(Position.MID, member.build().position());
                Assertions.assertEquals(List.of("crossbow", "breastplate", "leather_chausses", "boots",
                    "leather_helm", "leather_gloves"), member.build().itemCodes());
                Assertions.assertTrue(member.items(tier.level()).stream().allMatch(item ->
                    item.rarity() == ItemRarity.COMMON && item.modifier().isEmpty()
                ));
            }
            assertLegal(mobile);
        }
    }

    @Test
    void nominalBaseAndRarityGoldExcludesDesiredModifierAcquisitionAndStormShards() throws Exception {
        Assertions.assertEquals(2850, nominalGold(Step123ReferenceTeams.mainTeam(Tier.MID, 3)));
        Assertions.assertEquals(6250, nominalGold(Step123ReferenceTeams.mainTeam(Tier.MID, 7)));
        Assertions.assertEquals(13050, nominalGold(Step123ReferenceTeams.mainTeam(Tier.MID, 15)));
        Assertions.assertEquals(2550, nominalGold(Step123ReferenceTeams.specializedTeam(Tier.MID, Specialization.MOBILE)));
        Assertions.assertEquals(2850, nominalGold(Step123ReferenceTeams.specializedTeam(Tier.MID, Specialization.PRESSURE)));
        Assertions.assertEquals(3300, nominalGold(Step123ReferenceTeams.specializedTeam(Tier.MID, Specialization.FLANK)));
        for (final var tier : List.of(Tier.STRONG, Tier.EXTENDED, Tier.STRESS)) {
            Assertions.assertEquals(4725, nominalGold(Step123ReferenceTeams.mainTeam(tier, 3)));
            Assertions.assertEquals(9325, nominalGold(Step123ReferenceTeams.mainTeam(tier, 7)));
            Assertions.assertEquals(18525, nominalGold(Step123ReferenceTeams.mainTeam(tier, 15)));
            Assertions.assertEquals(3000, nominalGold(Step123ReferenceTeams.specializedTeam(tier, Specialization.MOBILE)));
            Assertions.assertEquals(4725, nominalGold(Step123ReferenceTeams.specializedTeam(tier, Specialization.PRESSURE)));
            Assertions.assertEquals(6450, nominalGold(Step123ReferenceTeams.specializedTeam(tier, Specialization.FLANK)));
        }
        Assertions.assertEquals(2100, nominalGold(Step123ReferenceTeams.mainTeam(Tier.BEGINNER, 3)));
        for (final var specialization : Specialization.values()) {
            Assertions.assertEquals(2100, nominalGold(Step123ReferenceTeams.specializedTeam(Tier.BEGINNER, specialization)));
        }
        for (final var tier : Tier.values()) {
            Assertions.assertEquals(2100,
                nominalGold(Step123ReferenceTeams.specializedTeam(tier, Specialization.COMMON_FRONT_PRESSURE)));
        }
    }

    @Test
    void commonFrontPressureIsAnExplicitZeroPointAlternativeAlongsideProfilePressureAndFlank() {
        for (final var tier : Tier.values()) {
            final var common = Step123ReferenceTeams.specializedTeam(tier, Specialization.COMMON_FRONT_PRESSURE);
            Assertions.assertEquals(List.of("GUARDIAN", "BRUISER", "BRUISER"), codes(common));
            common.members().forEach(member -> {
                Assertions.assertEquals(Position.FRONT, member.build().position());
                Assertions.assertTrue(member.items(tier.level()).stream().allMatch(item ->
                    item.rarity() == ItemRarity.COMMON && item.modifier().isEmpty() && item.skillPoints() == 0
                ));
            });
            Assertions.assertEquals(List.of("GUARDIAN", "BRUISER", "BRUISER"),
                codes(Step123ReferenceTeams.specializedTeam(tier, Specialization.PRESSURE)));
            Assertions.assertEquals(List.of("GUARDIAN", "ASSASSIN", "RAIDER"),
                codes(Step123ReferenceTeams.specializedTeam(tier, Specialization.FLANK)));
            for (final var specialization : Specialization.values()) {
                final var team = Step123ReferenceTeams.specializedTeam(tier, specialization);
                assertLegal(team);
                if (specialization == Specialization.PRESSURE || specialization == Specialization.FLANK) {
                    team.members().forEach(member -> Assertions.assertEquals(tier.profilePoints(),
                        member.items(tier.level()).stream().mapToInt(item -> item.skillPoints()).sum()));
                }
            }
        }
    }

    @Test
    void actualStormConfigurationAndItsFullPriceDomainFingerprintRemainFixed() throws Exception {
        final var economy = Step123ReferenceEconomy.configured();
        final String prefix = "homyakin.seeker.item.storm-enhance.";
        Assertions.assertEquals(Map.ofEntries(
            Map.entry(prefix + "bonus-percent-per-level", "5"), Map.entry(prefix + "base-cost", "10"),
            Map.entry(prefix + "cost-multiplier-numerator", "5"), Map.entry(prefix + "cost-multiplier-denominator", "4"),
            Map.entry(prefix + "base-success-percent", "100"), Map.entry(prefix + "success-multiplier", "0.75"),
            Map.entry(prefix + "failure-peak-level", "10"), Map.entry(prefix + "failure-base-weight", "15"),
            Map.entry(prefix + "failure-grow-multiplier", "1.1"), Map.entry(prefix + "failure-decay-multiplier", "0.65"),
            Map.entry(prefix + "rollback-from-level", "4"), Map.entry(prefix + "rollback-base-weight", "4"),
            Map.entry(prefix + "rollback-grow-multiplier", "1.25")
        ), economy.values());
        Assertions.assertEquals("aba8878bfe21ed645268b224b84a658b28906de16b35e0939932c0bfc2a1c836", economy.fingerprint());
        Assertions.assertEquals(0, economy.expectedAttemptsFromZero(0));
        Assertions.assertEquals(3.5035205758097323, economy.expectedAttemptsFromZero(3), 1e-12);
        Assertions.assertEquals(9.40017711076414, economy.expectedAttemptsFromZero(6), 1e-12);
        Assertions.assertEquals(54.60348666617736, economy.expectedAttemptsFromZero(10), 1e-10);
        Assertions.assertEquals(501.6386688877298, economy.expectedAttemptsFromZero(12), 1e-9);
        Assertions.assertEquals(663048.0690307205, economy.expectedAttemptsFromZero(15), 1e-6);
        Assertions.assertEquals(834141706446986.9, economy.expectedAttemptsFromZero(20), 1000);
        Assertions.assertThrows(IllegalArgumentException.class, () -> economy.expectedAttemptsFromZero(-1));
        for (final int level : List.of(0, 3, 6, 10, 12, 15, 20)) {
            Assertions.assertEquals(600 + 10 * level, ItemProgression.valueAtLevel(600, level));
        }
    }

    private static List<String> codes(ReferenceTeam team) {
        return team.members().stream().map(member -> member.build().code()).toList();
    }

    private static long nominalGold(ReferenceTeam team) throws Exception {
        return Step123ReferenceEconomy.configured().nominalGold(team.members().stream()
            .flatMap(member -> member.items(team.tier().level()).stream()).toList());
    }

    private static void assertLegal(ReferenceTeam team) {
        for (final var member : team.members()) {
            Assertions.assertEquals(EquipmentCatalogVersion.SCALING_V5, member.catalog());
            final var occupied = EnumSet.noneOf(PersonageSlot.class);
            for (final var item : member.items(team.tier().level())) {
                Assertions.assertEquals(ItemProgressionVersion.V1, item.object().progressionVersion());
                Assertions.assertEquals(team.tier().level(), item.enhanceLevel());
                item.object().slots().forEach(slot -> Assertions.assertTrue(occupied.add(slot)));
            }
            Assertions.assertEquals(EnumSet.allOf(PersonageSlot.class), occupied);
        }
    }
}
