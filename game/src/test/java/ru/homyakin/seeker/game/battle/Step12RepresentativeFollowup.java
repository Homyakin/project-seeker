package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Build;
import ru.homyakin.seeker.game.battle.Step12RepresentativeMatrix.Scenario;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.MixedBuild;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.RoleBuild;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.item.models.ItemRarity;

/** Questions selected after the first overview; answers use an independent random root. */
final class Step12RepresentativeFollowup {
    private Step12RepresentativeFollowup() {
    }

    static List<Scenario> scenarios() {
        final var result = new ArrayList<Scenario>();
        final var responses = new ArrayList<Build>();
        for (final var hands : List.of(
            List.of("sledgehammer"), List.of("two_handed_sword"), List.of("mace", "club"),
            List.of("staff", "orb"), List.of("command_staff"), List.of("sword", "tower_shield"), List.of("sword", "shield")
        )) {
            responses.add(Step12RepresentativeBuilds.hand(String.join("+", hands), hands,
                "LEATHER", Position.FRONT, TargetingTactic.THREAT));
        }
        responses.add(Step12RepresentativeBuilds.hand("COMMAND_NEW_LEATHER", List.of("command_staff"),
            "NEW_LEATHER", Position.FRONT, TargetingTactic.THREAT));
        for (final var main : List.of("sword", "rapier", "mace")) {
            final var leader = Step12RepresentativeBuilds.hand(main + "+dirk", List.of(main, "dirk"),
                "LEATHER", Position.FRONT, TargetingTactic.THREAT);
            for (final var response : responses) {
                for (final int level : List.of(0, 10, 20)) {
                    result.add(Scenario.of("RESPONSE_" + leader.code() + "_" + response.code(), "HAND_RESPONSE", level,
                        List.of(leader), List.of(response), 1000));
                }
            }
        }
        final var original = Step12RepresentativeBuilds.role(RoleBuild.SKIRMISHER);
        final var leather = original.withArmor("LEATHER").renamed("SKIRMISHER_LEATHER");
        final var slash = Step12RepresentativeBuilds.hand("SKIRMISHER_SLASH", List.of("sword", "shortsword"),
            "CLOTH", Position.FRONT, TargetingTactic.THREAT).withAssignments(Map.of(
                "cloth_boots", original.assignments().get("cloth_boots")
            ));
        for (final var skirmisher : List.of(original, leather, slash)) {
            for (final int level : List.of(0, 10, 20)) {
                for (final var opponent : Step12RepresentativeMatrix.opponents()) {
                    result.add(Scenario.of("NICHE_" + skirmisher.code() + "_" + opponent.code(), "SKIRMISHER_NICHE", level,
                        List.of(skirmisher), List.of(opponent), 1000));
                }
                for (final var role : List.of(RoleBuild.BRUISER, RoleBuild.BREAKER_PHYSICAL)) {
                    result.add(Scenario.of("NICHE_TEAM_" + skirmisher.code() + "_" + role, "SKIRMISHER_TEAM", level,
                        Step12RepresentativeMatrix.anchored(skirmisher),
                        Step12RepresentativeMatrix.anchored(Step12RepresentativeBuilds.role(role)), 1000));
                }
            }
        }
        for (final int level : List.of(0, 10, 20)) {
            for (final var tested : List.of(Step12RepresentativeBuilds.role(RoleBuild.ASSASSIN),
                Step12RepresentativeBuilds.mixed(MixedBuild.RAIDER))) {
                for (final var response : List.of(RoleBuild.BRUISER, RoleBuild.RANGER_MAGICAL)) {
                    result.add(Scenario.of("PROTECTED_" + tested.code() + "_" + response, "ACTIVE_SUPPORT", level,
                        protectedTeam(tested), protectedTeam(Step12RepresentativeBuilds.role(response)), 1000));
                }
            }
            final var guardian = Step12RepresentativeBuilds.role(RoleBuild.GUARDIAN);
            final var bruiser = Step12RepresentativeBuilds.role(RoleBuild.BRUISER);
            result.add(Scenario.of("PRESSURE_NO_SOFT_BACK", "ACTIVE_SUPPORT", level,
                List.of(guardian, Step12RepresentativeBuilds.role(RoleBuild.ASSASSIN),
                    Step12RepresentativeBuilds.mixed(MixedBuild.RAIDER)),
                List.of(guardian, bruiser, bruiser), 1000));
            for (final var skill : List.of(ActiveEnum.TEMPO_BREAK, ActiveEnum.KNOCKBACK)) {
                final var weapon = skill == ActiveEnum.TEMPO_BREAK ? "command_staff" : "bow";
                for (final var family : List.of("CLOTH", "NEW_CLOTH")) {
                    final var build = Step12RepresentativeBuilds.hand(skill + "_" + family,
                        List.of(weapon), family, Position.BACK, TargetingTactic.THREAT)
                        .withAssignments(Map.of(weapon,
                            new Step12RepresentativeBuilds.Assignment(skill, ItemRarity.RARE)));
                    for (final var opponent : List.of(
                        Step12RepresentativeMatrix.opponents().get(1), Step12RepresentativeMatrix.opponents().get(3)
                    )) {
                        final var group = "CLOTH_SKILL_" + skill + "_" + opponent.code();
                        result.add(Scenario.of(group + "_" + family, "CLOTH_NICHE", level,
                            Step12RepresentativeMatrix.anchored(build), Step12RepresentativeMatrix.anchored(opponent), 1000)
                            .withSeedGroup(group));
                    }
                }
            }
        }
        for (final var scenario : Step12RepresentativeMatrix.scenarios()) {
            if ((scenario.family().equals("RISK") && (scenario.code().startsWith("RISK09")
                || scenario.code().startsWith("RISK10")))
                || (scenario.family().equals("OVERVIEW") && (scenario.code().contains("ASSASSIN")
                    || scenario.code().contains("RAIDER")))) {
                result.add(scenario);
            }
        }
        result.sort(Comparator.comparing(Scenario::code));
        return List.copyOf(result);
    }

    private static List<Build> protectedTeam(Build tested) {
        final var protectedTested = tested.position() == Position.BACK ? tested.withPosition(Position.MID) : tested;
        return List.of(Step12RepresentativeBuilds.role(RoleBuild.GUARDIAN), protectedTested,
            Step12RepresentativeBuilds.role(RoleBuild.RANGER_PHYSICAL).withPosition(Position.MID));
    }
}
