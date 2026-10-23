package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import ru.homyakin.seeker.game.battle.Step123BattleRunner.Gear;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Assignment;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBuilds.Build;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.MixedBuild;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.RoleBuild;
import ru.homyakin.seeker.game.battle.simulation.Step12SimulationFixtures.V3Build;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;

/**
 * Explicit inputs for later PvE studies, not victorious teams or a power-based difficulty ladder.
 *
 * <p>A solo input is a bruiser, without a claim that it is the best solo build. Larger teams have
 * one front guardian followed by the fixed R,M,B cycle. B starts in front and R/M in the middle.
 * Thus size 3 is G,R,M; size 7 has 1G/2B/2R/2M; size 15 has 1G/4B/5R/5M.
 * Guard capacity belongs to the team; adding guardians would not multiply that shared capacity.
 * Composition never consults displayed power, opponents, outcomes or random selection.</p>
 *
 * <p>BEGINNER/MID/STRONG are working inputs at +0/+3/+6 and 0/2/4 profile points. EXTENDED/STRESS
 * retain +10/+20 as technical controls. These names promise neither acquisition time nor PvE wins.
 * A requested modifier is random when first added; nominal gold is not its full acquisition cost.</p>
 */
public final class Step123ReferenceTeams {
    private static final List<RoleBuild> CYCLE = List.of(
        RoleBuild.RANGER_PHYSICAL, RoleBuild.RANGER_MAGICAL, RoleBuild.BRUISER
    );

    private Step123ReferenceTeams() {
    }

    public static ReferenceTeam mainTeam(Tier tier, int size) {
        Objects.requireNonNull(tier, "Tier must be specified");
        if (size < 1 || size > 30) {
            throw new IllegalArgumentException("Reference team size must be in 1..30");
        }
        final var members = new ArrayList<Gear>();
        for (int index = 0; index < size; index++) {
            final var role = size == 1 ? RoleBuild.BRUISER
                : index == 0 ? RoleBuild.GUARDIAN : CYCLE.get((index - 1) % CYCLE.size());
            var build = Step12RepresentativeBuilds.role(role);
            if (role == RoleBuild.RANGER_PHYSICAL || role == RoleBuild.RANGER_MAGICAL) {
                build = build.withPosition(Position.MID);
            }
            members.add(gear(build, tier));
        }
        return new ReferenceTeam("MAIN_" + size + "_" + tier, tier, members);
    }

    public static ReferenceTeam specializedTeam(Tier tier, Specialization specialization) {
        Objects.requireNonNull(tier, "Tier must be specified");
        Objects.requireNonNull(specialization, "Specialization must be specified");
        final var guardian = Step12RepresentativeBuilds.role(RoleBuild.GUARDIAN);
        final var bruiser = Step12RepresentativeBuilds.role(RoleBuild.BRUISER);
        final List<Build> builds = switch (specialization) {
            case MOBILE -> {
                final var mobile = Step12RepresentativeBuilds.hand(
                    "MOBILE_SPEAR_LEATHER", List.of("spear", "dirk"), "LEATHER",
                    Position.FRONT, TargetingTactic.THREAT
                ).withAssignments(mobileAssignments(tier));
                final var support = Step123BattleStudy.historicalV3Build(V3Build.CROSSBOW_SUPPORT)
                    .withPosition(Position.MID);
                yield List.of(mobile, support, support);
            }
            case COMMON_FRONT_PRESSURE -> List.of(guardian, bruiser, bruiser).stream()
                .map(build -> build.withAssignments(Map.of())).toList();
            case PRESSURE -> List.of(guardian, bruiser, bruiser);
            case FLANK -> List.of(
                guardian, Step12RepresentativeBuilds.role(RoleBuild.ASSASSIN),
                Step12RepresentativeBuilds.mixed(MixedBuild.RAIDER)
            );
        };
        return new ReferenceTeam(specialization + "_" + tier, tier, builds.stream()
            .map(build -> specialization == Specialization.MOBILE || specialization == Specialization.COMMON_FRONT_PRESSURE
                ? new Gear(build, EquipmentCatalogVersion.SCALING_V5) : gear(build, tier)).toList());
    }

    private static Gear gear(Build build, Tier tier) {
        final var assignments = new java.util.LinkedHashMap<String, Assignment>();
        if (tier.profilePoints() > 0) {
            for (final var item : build.items(0)) {
                final var original = build.assignments().get(item.object().code());
                if (original != null) {
                    final int points = original.rarity().skillPoints() * tier.profilePoints() / 4;
                    final var rarity = java.util.Arrays.stream(ItemRarity.values())
                        .filter(value -> value.skillPoints() == points).findFirst().orElseThrow();
                    assignments.put(item.object().code(), new Assignment(original.skill(), rarity));
                }
            }
        }
        return new Gear(build.withAssignments(assignments), EquipmentCatalogVersion.SCALING_V5);
    }

    private static Map<String, Assignment> mobileAssignments(Tier tier) {
        return switch (tier.profilePoints()) {
            case 0 -> Map.of();
            case 2 -> Map.of("boots", new Assignment(ActiveEnum.HIT_AND_RUN, ItemRarity.RARE));
            case 4 -> Map.of(
                "boots", new Assignment(ActiveEnum.HIT_AND_RUN, ItemRarity.RARE),
                "leather_helm", new Assignment(ActiveEnum.HIT_AND_RUN, ItemRarity.RARE)
            );
            default -> throw new IllegalArgumentException("Unsupported profile points: " + tier.profilePoints());
        };
    }

    /** Writes only an explicitly requested new file; does not run battles, connect to a DB or activate a release. */
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || args[0].isBlank()) {
            throw new IllegalArgumentException("Specify one new JSON output path");
        }
        final var teams = new ArrayList<ReferenceTeam>();
        for (final var tier : Tier.values()) {
            for (final int size : List.of(3, 7, 15)) {
                teams.add(mainTeam(tier, size));
            }
            for (final var specialization : Specialization.values()) {
                teams.add(specializedTeam(tier, specialization));
            }
        }
        final var economy = Step123ReferenceEconomy.configured();
        final var exported = teams.stream().map(team -> new ExportedTeam(
            team.code(), team.tier(), team.tier().group(), team.tier().level(),
            team.members().stream().mapToInt(member -> member.items(team.tier().level()).stream()
                .mapToInt(Item::skillPoints).sum()).max().orElseThrow(),
            economy.expectedAttemptsFromZero(team.tier().level()), economy.nominalGold(team.members().stream()
                .flatMap(member -> member.items(team.tier().level()).stream()).toList()),
            team.members().stream().map(member -> new ExportedMember(
                member.build().code(), member.catalog(), member.build().position(), member.build().tactic(),
                member.items(team.tier().level())
            )).toList()
        )).toList();
        final var json = JsonMapper.builder().addModule(new Jdk8Module()).build();
        try (final var output = Files.newOutputStream(Path.of(args[0]), StandardOpenOption.CREATE_NEW)) {
            json.writerWithDefaultPrettyPrinter().writeValue(output, Map.of(
                "purpose", "Input baselines for later PvE studies; no victory, acquisition-time or full-cost claim",
                "composition", "N=1: B; N>=2: one G followed by R,M,B cycle; shared team Guard capacity",
                "workingTiers", List.of(Tier.BEGINNER, Tier.MID, Tier.STRONG),
                "technicalTiers", List.of(Tier.EXTENDED, Tier.STRESS),
                "nominalGoldScope", "Common bases plus one rarity-upgrade chain per carrier; "
                    + "excludes random desired-modifier acquisition and storm shards",
                "stormConfiguration", economy.values(), "stormEconomyFingerprint", economy.fingerprint(),
                "combat", "SCALING_SKILLS_V2", "teams", exported
            ));
        }
    }

    public enum Tier {
        BEGINNER(0, 0, "WORKING_REFERENCE"),
        MID(3, 2, "WORKING_REFERENCE"),
        STRONG(6, 4, "WORKING_REFERENCE"),
        EXTENDED(10, 4, "TECHNICAL_CONTROL"),
        STRESS(20, 4, "TECHNICAL_CONTROL");

        private final int level;
        private final int profilePoints;
        private final String group;

        Tier(int level, int profilePoints, String group) {
            this.level = level;
            this.profilePoints = profilePoints;
            this.group = group;
        }

        public int level() {
            return level;
        }

        /** Points for each profile role; common supports and COMMON_FRONT_PRESSURE retain zero. */
        public int profilePoints() {
            return profilePoints;
        }

        public String group() {
            return group;
        }
    }

    public enum Specialization {
        MOBILE,
        COMMON_FRONT_PRESSURE,
        PRESSURE,
        FLANK,
    }

    public record ReferenceTeam(String code, Tier tier, List<Gear> members) {
        public ReferenceTeam {
            Objects.requireNonNull(code);
            Objects.requireNonNull(tier);
            members = List.copyOf(members);
            if (members.isEmpty() || members.size() > 30) {
                throw new IllegalArgumentException("Reference team size must be in 1..30");
            }
        }

        /** Creates fresh battle state; callers may wrap this in RandomUtils.withSeed when they need fixed UUIDs. */
        public List<BattlePersonage> personages() {
            return members.stream().map(member -> {
                final var build = member.build();
                final var personage = BattlePersonage.forScalingSkillsV2Combat(
                    member.items(tier.level()), build.position(), Map.of(), Optional.of(build.code())
                );
                personage.setTargetingTactic(build.tactic());
                return personage;
            }).toList();
        }
    }

    private record ExportedTeam(String code, Tier tier, String group, int enhanceLevel, int maximumMemberSkillPoints,
                                double expectedAttemptsPerItemFromZero, long nominalEquipmentGold,
                                List<ExportedMember> members) {
    }

    private record ExportedMember(String code, EquipmentCatalogVersion catalog, Position position,
                                  TargetingTactic tactic, List<Item> items) {
    }
}
