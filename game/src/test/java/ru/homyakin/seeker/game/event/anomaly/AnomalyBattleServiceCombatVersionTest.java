package ru.homyakin.seeker.game.event.anomaly;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.BattleResult;
import ru.homyakin.seeker.game.battle.BattleTempoVersion;
import ru.homyakin.seeker.game.battle.EventBattleLogService;
import ru.homyakin.seeker.game.battle.PlayerCombatConfig;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.event.anomaly.action.AnomalyBattleService;
import ru.homyakin.seeker.game.event.anomaly.entity.Anomaly;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyConfig;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyGvgStorage;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyPveTemplate;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyReward;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyStorage;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.item.loadout.action.EquipmentLoadoutService;
import ru.homyakin.seeker.game.item.loadout.entity.ResolvedCombatGear;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.event.EventParticipant;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.TimeUtils;

class AnomalyBattleServiceCombatVersionTest {
    @ParameterizedTest
    @MethodSource("routesTemplatesAndSizes")
    void realRoutesSelectEachTemplateAndResolveOnlyEligibleLoadoutPlayers(
        boolean safePve, AnomalyPveTemplate template, int count
    ) {
        final var fixture = new PveFixture(safePve, template, 6, true, count);
        final var result = fixture.fight();
        Mockito.verify(fixture.loadouts).resolveCombatGear(fixture.selectedPersonages, EventType.ANOMALY);
        Mockito.verifyNoInteractions(fixture.gvgStorage, fixture.anomalyStorage);
        Assertions.assertEquals(BattleTempoVersion.ACCELERATED_V1, result.initState().tempoVersion());
        final var snapshots = result.initState().personagesById().values();
        final var enemies = snapshots.stream().filter(snapshot -> snapshot.firstTeam()).toList();
        if (count == 2) {
            Assertions.assertTrue(enemies.size() == 2 || enemies.size() == 3);
        } else {
            Assertions.assertEquals(Math.max(2, count), enemies.size());
        }
        Assertions.assertEquals(count + enemies.size(), snapshots.size(),
            "A fallback excludes people now outside the initiating group");
        for (final var enemy : enemies) {
            Assertions.assertEquals(Set.of(template.attackType()), enemy.attacksByRange().getFirst().attack().keySet());
            Assertions.assertEquals(Set.of(template.defenseType()), enemy.defenses().keySet());
            Assertions.assertEquals(4, enemy.skills().getFirst().points());
            Assertions.assertEquals(SkillFormulaVersion.SCALING_SKILLS_V3, enemy.skills().getFirst().formulaVersion());
            final var actualLine = result.initState().lines().get(enemy.lineIndex());
            Assertions.assertEquals(switch (template) {
                case IRON_PHALANX -> Position.FRONT;
                case ASH_WIND -> enemy.range() == 1 ? Position.FRONT : Position.MID;
                case SHADOW_NEEDLES -> enemy.range() == 1 ? Position.FRONT : Position.BACK;
                case CRYSTAL_STORM -> enemy.range() == 2 ? Position.FRONT : Position.BACK;
            }, actualLine.position());
        }
        final var player = snapshots.stream().filter(snapshot -> !snapshot.firstTeam()).findFirst().orElseThrow();
        final var expected = BattlePersonage.forCombat(List.of(fixture.item.withoutStormEnhance()), Position.BACK,
            PersonageEffects.EMPTY, Optional.empty(), SkillFormulaVersion.SCALING_SKILLS_V3);
        Assertions.assertEquals(expected.maxHealth(), player.health());
        Assertions.assertEquals(expected.attacksByRange(), player.attacksByRange());
        Assertions.assertEquals(expected.defenses(), player.defenses());
        Assertions.assertEquals(expected.critChance(), player.critChance());
        Assertions.assertEquals(expected.dodgeChance(), player.dodgeChance());
        Assertions.assertEquals(expected.critMultiplier(), player.critMultiplier());
        Assertions.assertEquals(expected.initiative(), player.initiative());
        Assertions.assertEquals(Position.FRONT, fixture.personage.position());
        Assertions.assertEquals(Position.BACK, result.initState().lines().get(player.lineIndex()).position(),
            "The selected loadout position is used rather than the profile position");
        Assertions.assertEquals(TargetingTactic.EXECUTIONER, player.targetingTactic());
        Assertions.assertEquals(ItemRarity.LEGENDARY, player.items().getFirst().rarity());
        Assertions.assertEquals(Optional.of(ActiveEnum.BERSERK), player.items().getFirst().skill());
        Assertions.assertEquals(4, player.skills().getFirst().points());
        Assertions.assertEquals(6, fixture.item.enhanceLevel());
        Assertions.assertSame(fixture.item, fixture.gear.items().getFirst());
        Assertions.assertFalse(fixture.personage.effects().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("routesAndTemplates")
    void enhancementAndTemporaryEffectsDoNotChangePreparedBattleOrItsOutcome(
        boolean safePve, AnomalyPveTemplate template
    ) {
        final var enhanced = new PveFixture(safePve, template, 6, true);
        final var plain = new PveFixture(safePve, template, 0, false);
        final var first = enhanced.fight();
        final var second = plain.fight();
        Assertions.assertEquals(first.initState(), second.initState());
        Assertions.assertEquals(first.actionLog().events(), second.actionLog().events());
        Assertions.assertEquals(first.actionLog().traceEvents(), second.actionLog().traceEvents());
        Assertions.assertEquals(first.rounds(), second.rounds());
        Assertions.assertEquals(first.firstWin(), second.firstWin());
        Assertions.assertEquals(first.personageStats(), second.personageStats());
        Assertions.assertEquals(6, enhanced.item.enhanceLevel());
        Assertions.assertEquals(0, plain.item.enhanceLevel());
        Assertions.assertFalse(enhanced.personage.effects().isEmpty());
        Assertions.assertTrue(plain.personage.effects().isEmpty());
    }

    @Test
    void emptyFallbackAfterMembershipFilteringFailsBeforeBattleLogPayoutOrStatusChanges() {
        final var fixture = new PveFixture(false, AnomalyPveTemplate.ASH_WIND, 6, true);
        final var outsider = participant(GroupId.from(15));
        Mockito.when(fixture.participants.getParticipants(fixture.event.id()))
            .thenReturn(List.of(new EventParticipant(outsider, Optional.empty())));
        Mockito.when(fixture.loadouts.resolveCombatGear(List.of(), EventType.ANOMALY)).thenReturn(Map.of());
        Assertions.assertThrows(IllegalArgumentException.class, () ->
            RandomUtils.withSeed(fixture.seed, () -> fixture.service.fightPveFallback(fixture.event, fixture.group))
        );
        Mockito.verify(fixture.loadouts).resolveCombatGear(List.of(), EventType.ANOMALY);
        Mockito.verifyNoInteractions(fixture.logs, fixture.personages, fixture.events,
            fixture.rewards, fixture.gvgStorage, fixture.anomalyStorage);
    }

    private static Stream<Arguments> routesAndTemplates() {
        return Stream.of(AnomalyPveTemplate.values()).flatMap(template -> Stream.of(
            Arguments.of(true, template), Arguments.of(false, template)
        ));
    }

    private static Stream<Arguments> routesTemplatesAndSizes() {
        return Stream.of(AnomalyPveTemplate.values()).flatMap(template -> Stream.of(true, false)
            .flatMap(safe -> IntStream.rangeClosed(1, 5).mapToObj(count -> Arguments.of(safe, template, count))));
    }

    private static final class PveFixture {
        private final PersonageEventService participants = Mockito.mock(PersonageEventService.class);
        private final PersonageService personages = Mockito.mock(PersonageService.class);
        private final EquipmentLoadoutService loadouts = Mockito.mock(EquipmentLoadoutService.class);
        private final EventBattleLogService logs = Mockito.mock(EventBattleLogService.class);
        private final LaunchedEventService events = Mockito.mock(LaunchedEventService.class);
        private final AnomalyConfig rewards = Mockito.mock(AnomalyConfig.class);
        private final AnomalyGvgStorage gvgStorage = Mockito.mock(AnomalyGvgStorage.class);
        private final AnomalyStorage anomalyStorage = Mockito.mock(AnomalyStorage.class);
        private final GroupId group = GroupId.from(14);
        private final Personage personage;
        private final List<Personage> selectedPersonages;
        private final Item item;
        private final ResolvedCombatGear gear;
        private final LaunchedEvent event;
        private final AnomalyBattleService service;
        private final AnomalyPveTemplate template;
        private final boolean safePve;
        private final long seed;

        private PveFixture(boolean safePve, AnomalyPveTemplate template, int enhance, boolean withEffects) {
            this(safePve, template, enhance, withEffects, 1);
        }

        private PveFixture(boolean safePve, AnomalyPveTemplate template, int enhance, boolean withEffects, int count) {
            this.safePve = safePve;
            this.template = template;
            this.seed = safePve ? 202611150019L : IntStream.range(0, 100_000)
                .filter(candidate -> RandomUtils.withSeed(candidate, AnomalyPveTemplate::random) == template)
                .findFirst().orElseThrow();
            this.selectedPersonages = IntStream.range(0, count).mapToObj(index -> {
                final var base = participant(group, 14 + index);
                return new Personage(base.id(), "anomaly service participant " + index,
                    base.tag(), base.memberGroupId(), base.money(), base.stormShards(), base.energy(), base.badge(),
                    withEffects ? base.effects() : PersonageEffects.EMPTY, Position.FRONT,
                    TargetingTactic.THREAT, base.onlineStreak());
            }).toList();
            this.personage = selectedPersonages.getFirst();
            this.item = new Item(new ItemObject("selected-anomaly-loadout", Set.of(PersonageSlot.MAIN_HAND),
                Optional.of(new ItemAttack(AttackType.SLASH, 3, 100)),
                Optional.of(new ItemDefense(DefenseType.LEATHER, 100)), 300, 5, 5, 0.4, 180, 20, null
            ), Optional.of(new Modifier(ActiveEnum.BERSERK)), ItemRarity.LEGENDARY, enhance);
            this.gear = new ResolvedCombatGear(List.of(item), Position.BACK, TargetingTactic.EXECUTIONER);
            final var now = TimeUtils.moscowTime();
            this.event = new LaunchedEvent(14, 1, now, now.plusHours(1), EventStatus.LAUNCHED, Optional.empty());
            final var outsideGroup = participant(GroupId.from(15), 114);
            final var noGroup = PersonageUtils.withId(PersonageId.from(115));
            final var eligibleParticipants = selectedPersonages.stream()
                .map(selected -> new EventParticipant(selected, Optional.empty())).toList();
            Mockito.when(participants.getParticipants(event.id())).thenReturn(safePve ? eligibleParticipants
                : Stream.concat(eligibleParticipants.stream(), Stream.of(
                    new EventParticipant(outsideGroup, Optional.empty()), new EventParticipant(noGroup, Optional.empty())
                )).toList());
            Mockito.when(loadouts.resolveCombatGear(selectedPersonages, EventType.ANOMALY))
                .thenReturn(selectedPersonages.stream().collect(java.util.stream.Collectors.toMap(
                    Personage::id, _ -> gear
                )));
            final var noReward = AnomalyReward.of(0, 0);
            Mockito.when(rewards.pveWinReward()).thenReturn(noReward);
            Mockito.when(rewards.pveLoseReward()).thenReturn(noReward);
            Mockito.when(rewards.gvgFallbackWinReward()).thenReturn(noReward);
            Mockito.when(rewards.gvgFallbackLoseReward()).thenReturn(noReward);
            final var config = new PlayerCombatConfig();
            config.setVersion(SkillFormulaVersion.SCALING_SKILLS_V3);
            config.setTempoVersion(BattleTempoVersion.ACCELERATED_V1);
            this.service = new AnomalyBattleService(participants, personages, loadouts, logs,
                events, rewards, gvgStorage, anomalyStorage, config);
        }

        private BattleResult fight() {
            RandomUtils.withSeed(seed, () -> safePve
                ? service.fightPve(event, new Anomaly.Safe(event.id(), group, personage.id(), template,
                    Anomaly.Safe.Phase.PVE_WAITING))
                : service.fightPveFallback(event, group));
            final var result = ArgumentCaptor.forClass(BattleResult.class);
            Mockito.verify(logs).save(Mockito.eq(event.id()), result.capture());
            if (safePve) {
                if (result.getValue().firstWin()) {
                    Mockito.verify(rewards).pveLoseReward();
                } else {
                    Mockito.verify(rewards).pveWinReward();
                }
            } else if (result.getValue().firstWin()) {
                Mockito.verify(rewards).gvgFallbackLoseReward();
            } else {
                Mockito.verify(rewards).gvgFallbackWinReward();
            }
            Mockito.verifyNoMoreInteractions(rewards);
            Mockito.verify(events).updateStatus(event.id(),
                result.getValue().firstWin() ? EventStatus.FAILED : EventStatus.SUCCESS);
            return result.getValue();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void bothPveRoutesUseCurrentNpcSkillsAndStripOnlyBattleCopies(boolean safePve) {
        final var participants = Mockito.mock(PersonageEventService.class);
        final var personages = Mockito.mock(PersonageService.class);
        final var loadouts = Mockito.mock(EquipmentLoadoutService.class);
        final var logs = Mockito.mock(EventBattleLogService.class);
        final var events = Mockito.mock(LaunchedEventService.class);
        final var rewards = Mockito.mock(AnomalyConfig.class);
        final var config = new PlayerCombatConfig();
        config.setVersion(SkillFormulaVersion.SCALING_SKILLS_V3);
        config.setTempoVersion(BattleTempoVersion.ACCELERATED_V1);
        final var service = new AnomalyBattleService(
            participants,
            personages,
            loadouts,
            logs,
            events,
            rewards,
            Mockito.mock(AnomalyGvgStorage.class),
            Mockito.mock(AnomalyStorage.class),
            config
        );
        final var group = GroupId.from(14);
        final var personage = participant(group);
        final var item = enhancedItem();
        final var gear = new ResolvedCombatGear(List.of(item), Position.FRONT, TargetingTactic.EXECUTIONER);
        final var now = TimeUtils.moscowTime();
        final var event = new LaunchedEvent(14, 1, now, now.plusHours(1), EventStatus.LAUNCHED, Optional.empty());
        Mockito.when(participants.getParticipants(event.id()))
            .thenReturn(List.of(new EventParticipant(personage, Optional.empty())));
        Mockito.when(loadouts.resolveCombatGear(List.of(personage), EventType.ANOMALY))
            .thenReturn(Map.of(personage.id(), gear));
        final var noReward = AnomalyReward.of(0, 0);
        Mockito.when(rewards.pveWinReward()).thenReturn(noReward);
        Mockito.when(rewards.pveLoseReward()).thenReturn(noReward);
        Mockito.when(rewards.gvgFallbackWinReward()).thenReturn(noReward);
        Mockito.when(rewards.gvgFallbackLoseReward()).thenReturn(noReward);

        RandomUtils.withSeed(2026110714L, () -> safePve
            ? service.fightPve(event, new Anomaly.Safe(
                event.id(), group, personage.id(), AnomalyPveTemplate.CRYSTAL_STORM, Anomaly.Safe.Phase.PVE_WAITING
            ))
            : service.fightPveFallback(event, group)
        );

        final var result = ArgumentCaptor.forClass(BattleResult.class);
        Mockito.verify(logs).save(Mockito.eq(event.id()), result.capture());
        Assertions.assertEquals(BattleTempoVersion.ACCELERATED_V1, result.getValue().initState().tempoVersion());
        final var snapshots = result.getValue().initState().personagesById().values();
        Assertions.assertEquals(3, snapshots.size());
        for (final var snapshot : snapshots) {
            Assertions.assertEquals(1, snapshot.skills().size());
            Assertions.assertEquals(4, snapshot.skills().getFirst().points());
            Assertions.assertEquals(
                SkillFormulaVersion.SCALING_SKILLS_V3,
                snapshot.skills().getFirst().formulaVersion()
            );
        }
        final var player = snapshots.stream().filter(snapshot -> !snapshot.firstTeam()).findFirst().orElseThrow();
        final var expected = BattlePersonage.forCombat(
            List.of(item.withoutStormEnhance()), Position.FRONT, PersonageEffects.EMPTY, Optional.empty(), config.version()
        );
        Assertions.assertEquals(expected.maxHealth(), player.health(), "Anomaly ignores enhancement and active effects");
        Assertions.assertEquals(item.object().health(), player.health());
        Assertions.assertEquals(item.object().attacks().getFirst().attack(),
            player.attacksByRange().getFirst().attack().get(AttackType.SLASH));
        Assertions.assertEquals(TargetingTactic.EXECUTIONER, player.targetingTactic());
        Assertions.assertEquals(ItemRarity.LEGENDARY, player.items().getFirst().rarity());
        Assertions.assertEquals(Optional.of(ActiveEnum.BERSERK), player.items().getFirst().skill());
        Assertions.assertEquals(6, item.enhanceLevel(), "The stored/loadout item must not be mutated");
        Assertions.assertSame(item, gear.items().getFirst());
        Assertions.assertFalse(personage.effects().isEmpty(), "Original effects remain untouched");
    }

    private static Personage participant(GroupId group) {
        return participant(group, 14);
    }

    private static Personage participant(GroupId group, long id) {
        final var base = PersonageUtils.withId(PersonageId.from(id));
        return new Personage(
            base.id(), base.name(), base.tag(), Optional.of(group), base.money(), base.stormShards(),
            base.energy(), base.badge(), new PersonageEffects(Map.of(
                PersonageEffectType.CONTRABAND_BUFF,
                new PersonageEffect(new Effect.Add(777, EffectCharacteristic.HEALTH), TimeUtils.moscowTime().plusHours(1))
            )), Position.FRONT, TargetingTactic.EXECUTIONER, base.onlineStreak()
        );
    }

    private static Item enhancedItem() {
        return new Item(new ItemObject(
            "anomaly-version-test", Set.of(PersonageSlot.MAIN_HAND), Optional.of(new ItemAttack(AttackType.SLASH, 1, 100)),
            Optional.of(new ItemDefense(DefenseType.LEATHER, 100)), 300, 5, 5, 0.4, 180, 20, null
        ), Optional.of(new Modifier(ActiveEnum.BERSERK)), ItemRarity.LEGENDARY, 6);
    }
}
