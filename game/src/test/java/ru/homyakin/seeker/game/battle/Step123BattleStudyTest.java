package ru.homyakin.seeker.game.battle;

import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V3Build;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;

class Step123BattleStudyTest {
    @Test
    void casesAreUniqueAndUseCompleteLegalEquipment() {
        final var cases = Step123BattleStudy.cases();
        Assertions.assertThat(cases).hasSize(1203);
        Assertions.assertThat(cases.stream().map(Step123BattleStudy.StudyCase::code).distinct().count())
            .isEqualTo(cases.size());
        for (final var value : cases) {
            for (final var team : List.of(value.left(), value.right())) {
                for (final var gear : team) {
                    Assertions.assertThat(gear.items(value.level()).stream()
                        .mapToInt(item -> item.object().slots().size()).sum()).isEqualTo(7);
                }
            }
        }
    }

    @Test
    void mobileAlternativesKeepFourPointsAndAnExactOffComparison() {
        for (final var build : Step123BattleStudy.mobileBuilds()) {
            Assertions.assertThat(build.items(0).stream().mapToInt(item -> item.skillPoints()).sum()).isEqualTo(4);
            Assertions.assertThat(build.assignments().values()).allMatch(value -> value.skill() == ActiveEnum.HIT_AND_RUN);
        }
        final var cases = Step123BattleStudy.cases();
        for (final var value : cases.stream().filter(value -> value.family().startsWith("MOBILE")
            && value.disabledLeft().isEmpty()).toList()) {
            final var off = cases.stream().filter(candidate -> candidate.code().equals(value.code() + "_OFF"))
                .findFirst().orElseThrow();
            Assertions.assertThat(off.left()).isEqualTo(value.left());
            Assertions.assertThat(off.right()).isEqualTo(value.right());
            Assertions.assertThat(off.seedGroup()).isEqualTo(value.seedGroup());
            Assertions.assertThat(off.disabledLeft()).containsExactly(ActiveEnum.HIT_AND_RUN);
        }
    }

    @Test
    void clothComparisonsHaveTheSameKnownGoldCost() throws Exception {
        final var selected = Step123BattleStudy.cases().stream().filter(value -> value.family().equals("CLOTH")).toList();
        for (final var value : selected) {
            Assertions.assertThat(Step123BattleStudy.knownGold(value.left(), value.level())).isEqualTo(2550);
            Assertions.assertThat(Step123BattleStudy.knownGold(value.right(), value.level())).isEqualTo(2100);
        }
    }

    @Test
    void historicalNicheUsesActualV3EquipmentAndIdenticalMidlineCrossbowSupports() {
        final var cases = Step123BattleStudy.cases();
        final var niche = cases.stream().filter(value -> value.family().equals("MOBILE_NICHE")).toList();
        Assertions.assertThat(niche).hasSize(108);
        Assertions.assertThat(cases.stream().filter(value -> !value.family().equals("MOBILE_NICHE")
            && !value.family().equals("MOBILE_ANSWER")).count())
            .isEqualTo(1077);
        Assertions.assertThat(Step123BattleStudy.mobileNicheBuilds()).hasSize(6);
        final var historical = Step123BattleStudy.historicalV3Build(V3Build.SKIRMISHER_SLASH);
        Assertions.assertThat(historical.items(0))
            .isEqualTo(Step12SimulationFixtures.v3Items(V3Build.SKIRMISHER_SLASH, 0));
        Assertions.assertThat(historical.assignments().get("cloth_boots").rarity()).isEqualTo(ItemRarity.LEGENDARY);
        Assertions.assertThat(historical.items(0).stream().mapToInt(item -> item.skillPoints()).sum()).isEqualTo(4);
        final var support = Step123BattleStudy.historicalV3Build(V3Build.CROSSBOW_SUPPORT);
        Assertions.assertThat(support.items(0))
            .isEqualTo(Step12SimulationFixtures.v3Items(V3Build.CROSSBOW_SUPPORT, 0));
        for (final var value : niche) {
            Assertions.assertThat(value.left()).hasSize(3);
            Assertions.assertThat(value.right()).hasSize(3);
            Assertions.assertThat(value.left().getFirst().build().position()).isEqualTo(Position.FRONT);
            Assertions.assertThat(value.right().getFirst().build().position()).isEqualTo(Position.FRONT);
            Assertions.assertThat(value.left().getFirst().build().items(0).stream()
                .mapToInt(item -> item.skillPoints()).sum()).isEqualTo(4);
            for (final var team : List.of(value.left(), value.right())) {
                Assertions.assertThat(team).allMatch(gear -> gear.catalog() == EquipmentCatalogVersion.SCALING_V4);
                Assertions.assertThat(team.get(1).build()).isEqualTo(support);
                Assertions.assertThat(team.get(2).build()).isEqualTo(support);
                Assertions.assertThat(team.get(1).build().position()).isEqualTo(Position.MID);
                Assertions.assertThat(team.get(2).build().position()).isEqualTo(Position.MID);
                Assertions.assertThat(team.get(1).build().assignments()).isEmpty();
            }
        }
        final var historicalPair = niche.stream().filter(value -> value.level() == 0 && value.disabledLeft().isEmpty()
            && value.left().getFirst().build().equals(historical)
            && value.right().getFirst().build().code().endsWith("BREAKER_CLOSE_SLASH")).findFirst().orElseThrow();
        Assertions.assertThat(historicalPair.right().getFirst().build().items(0))
            .isEqualTo(Step12SimulationFixtures.v3Items(V3Build.BREAKER_CLOSE_SLASH, 0));
        Assertions.assertThat(niche.stream().map(Step123BattleStudy.StudyCase::level).collect(java.util.stream.Collectors.toSet()))
            .isEqualTo(Set.of(0, 10, 20));
    }

    @Test
    void priorMobileSeedGroupsRemainStableAfterAddingTheIndependentNicheFamily() {
        for (final var value : Step123BattleStudy.cases().stream()
            .filter(value -> value.family().equals("MOBILE")).toList()) {
            final var expectedGroup = value.code().substring(0, value.code().lastIndexOf("_L"));
            Assertions.assertThat(value.seedGroup()).isEqualTo(expectedGroup);
        }
        final var unchanged = Step123BattleStudy.cases().stream()
            .filter(value -> value.code().equals("MOBILE_sword_dirk_LEATHER_SOLE_FRONT_BRUISER_L10"))
            .findFirst().orElseThrow();
        Assertions.assertThat(unchanged.seedGroup()).isEqualTo("MOBILE_sword_dirk_LEATHER_SOLE_FRONT_BRUISER");
    }

    @Test
    void mobileAnswersKeepTheAcceptedNicheTeamUnchangedAndMakeCostsAndResponsesExplicit() throws Exception {
        final var cases = Step123BattleStudy.cases();
        final var answers = cases.stream().filter(value -> value.family().equals("MOBILE_ANSWER")).toList();
        Assertions.assertThat(answers).hasSize(18);
        Assertions.assertThat(cases.stream().filter(value -> !value.family().equals("MOBILE_ANSWER")).count())
            .isEqualTo(1185);
        for (final var value : answers) {
            final var acceptedNiche = cases.stream().filter(candidate -> candidate.family().equals("MOBILE_NICHE")
                && candidate.level() == value.level() && candidate.disabledLeft().isEmpty()
                && candidate.left().getFirst().build().code().equals("MOBILE_spear_dirk_LEATHER"))
                .findFirst().orElseThrow();
            Assertions.assertThat(value.left()).isEqualTo(acceptedNiche.left());
            Assertions.assertThat(value.left().stream().map(gear -> gear.items(value.level())).toList())
                .isEqualTo(acceptedNiche.left().stream().map(gear -> gear.items(value.level())).toList());
            Assertions.assertThat(Step123BattleStudy.knownGold(value.left(), value.level())).isEqualTo(3000);
            Assertions.assertThat(value.left().stream().flatMap(gear -> gear.items(value.level()).stream())
                .mapToInt(item -> item.skillPoints()).sum()).isEqualTo(4);
            Assertions.assertThat(value.right()).allMatch(gear -> gear.catalog() == EquipmentCatalogVersion.SCALING_V4);
            if (value.seedGroup().endsWith("A_ANCHORED_BRUISER")) {
                Assertions.assertThat(value.right().stream().map(gear -> gear.build().code()).toList())
                    .containsExactly("NEUTRAL_FRONT", "BRUISER", "NEUTRAL_BACK");
                Assertions.assertThat(value.right().stream().map(gear -> gear.build().position()).toList())
                    .containsExactly(Position.FRONT, Position.FRONT, Position.BACK);
                Assertions.assertThat(value.right().getFirst().build().assignments()).isEmpty();
                Assertions.assertThat(value.right().getLast().build().assignments()).isEmpty();
                Assertions.assertThat(Step123BattleStudy.knownGold(value.right(), value.level())).isEqualTo(2550);
            } else if (value.seedGroup().endsWith("B_FRONT_PRESSURE")) {
                Assertions.assertThat(value.right().stream().map(gear -> gear.build().code()).toList())
                    .containsExactly("GUARDIAN", "BRUISER", "BRUISER");
                Assertions.assertThat(value.right()).allMatch(gear -> gear.build().position() == Position.FRONT);
                Assertions.assertThat(Step123BattleStudy.knownGold(value.right(), value.level())).isEqualTo(4725);
            } else {
                Assertions.assertThat(value.seedGroup()).endsWith("C_PROTECTED_RANGERS");
                Assertions.assertThat(value.right().stream().map(gear -> gear.build().code()).toList())
                    .containsExactly("GUARDIAN", "RANGER_PHYSICAL", "RANGER_MAGICAL");
                Assertions.assertThat(value.right().stream().map(gear -> gear.build().position()).toList())
                    .containsExactly(Position.FRONT, Position.MID, Position.MID);
                Assertions.assertThat(Step123BattleStudy.knownGold(value.right(), value.level())).isEqualTo(4725);
            }
        }
    }
}
