package ru.homyakin.seeker.game.event.anomaly;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
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
        final var base = PersonageUtils.withId(PersonageId.from(14));
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
