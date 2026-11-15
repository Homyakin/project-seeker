package ru.homyakin.seeker.game.event.raid.processing;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.EventBattleLogService;
import ru.homyakin.seeker.game.battle.PlayerCombatConfig;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.launched.LaunchedEventService;
import ru.homyakin.seeker.game.event.launched.RaidParams;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.event.raid.RaidService;
import ru.homyakin.seeker.game.event.raid.generator.RaidGenerator;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.raid.models.RaidPersonageParams;
import ru.homyakin.seeker.game.event.raid.models.RaidType;
import ru.homyakin.seeker.game.event.service.GroupEventService;
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
import ru.homyakin.seeker.game.outpost.action.GroupPassiveEffectsService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.event.PersonageEventService;
import ru.homyakin.seeker.game.personage.event.RaidParticipant;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType;
import ru.homyakin.seeker.game.personage.power.ReferencePowerCalculator;

class RaidProcessingCombatVersionTest {
    @ParameterizedTest
    @EnumSource(value = SkillFormulaVersion.class, names = {"LEGACY_SKILLS_V1", "SCALING_SKILLS_V3"})
    void productionCallerPassesSameVersionAndPreservesEnhancedEquipmentAndEffects(SkillFormulaVersion version) {
        final var raidService = Mockito.mock(RaidService.class);
        final var participantService = Mockito.mock(PersonageEventService.class);
        final var loadoutService = Mockito.mock(EquipmentLoadoutService.class);
        final var generator = Mockito.mock(RaidGenerator.class);
        final var personageService = Mockito.mock(PersonageService.class);
        final var eventLog = Mockito.mock(EventBattleLogService.class);
        final var config = new PlayerCombatConfig();
        config.setVersion(version);
        final var processing = new RaidProcessing(
            personageService,
            loadoutService,
            raidService,
            Mockito.mock(RaidItemGenerator.class),
            generator,
            Mockito.mock(RaidRewardGenerator.class),
            Mockito.mock(LaunchedEventService.class),
            participantService,
            Mockito.mock(GroupEventService.class),
            Mockito.mock(GroupPassiveEffectsService.class),
            eventLog,
            config
        );
        final var item = new Item(new ItemObject(
            null,
            Set.of(PersonageSlot.MAIN_HAND),
            Optional.of(new ItemAttack(AttackType.PIERCE, 1, 100)),
            Optional.of(new ItemDefense(DefenseType.LEATHER, 100)),
            1000, 5, 5, 0.5, 200, 10, null
        ), Optional.of(new Modifier(ActiveEnum.BLEEDING)), ItemRarity.LEGENDARY, 3);
        final var effects = new PersonageEffects(Map.of(PersonageEffectType.MENU_ITEM_EFFECT,
            new PersonageEffect(new Effect.Multiplier(50, EffectCharacteristic.HEALTH), null)));
        final var personage = new Personage(
            PersonageId.from(14), "Participant", Optional.empty(), Optional.empty(),
            null, null, null, BadgeView.STANDARD, effects, Position.FRONT, TargetingTactic.THREAT, null
        );
        final var participant = new RaidParticipant(personage, new RaidPersonageParams(false));
        final var gear = new ResolvedCombatGear(List.of(item), Position.MID, TargetingTactic.WOUNDED_HUNTER);
        final var start = LocalDateTime.of(2026, 11, 7, 14, 0);
        final var launched = new LaunchedEvent(
            14, 1, start, start.plusHours(1), EventStatus.LAUNCHED, Optional.of(new RaidParams(10, 0))
        );
        Mockito.when(raidService.getByEventId(1)).thenReturn(Optional.of(new Raid(1, "wolfpack", Map.of())));
        Mockito.when(participantService.getRaidParticipants(14)).thenReturn(List.of(participant));
        Mockito.when(loadoutService.resolveCombatGear(List.of(personage), EventType.RAID))
            .thenReturn(Map.of(personage.id(), gear));
        final var stopAfterGeneration = new IllegalStateException("Stop before battle and external writes");
        Mockito.doAnswer(invocation -> {
            Assertions.assertEquals(RaidType.WOLFPACK, invocation.getArgument(0));
            Assertions.assertEquals(version, invocation.getArgument(3));
            final List<BattlePersonage> players = invocation.getArgument(2);
            Assertions.assertEquals(1, players.size());
            final var player = players.getFirst();
            final var expected = BattlePersonage.forCombat(
                gear.items(), gear.battlePosition(), effects, Optional.empty(), version
            );
            Assertions.assertEquals(expected.maxHealth(), player.maxHealth());
            Assertions.assertTrue(player.maxHealth() > item.health(), "Raid keeps the active health effect");
            Assertions.assertEquals(ReferencePowerCalculator.calculate(gear.items(), 1).unscaled(),
                player.equipmentPowerUnscaled(), "Raid power uses the permanent resolved RAID loadout");
            Assertions.assertEquals(BattlePersonage.forCombat(
                gear.items(), gear.battlePosition(), PersonageEffects.EMPTY, Optional.empty(), version
            ).equipmentPowerUnscaled(), player.equipmentPowerUnscaled(),
                "Temporary combat effects must not alter the equipment power passed to the generator");
            Assertions.assertEquals(expected.battlePersonageStats().attack(), player.battlePersonageStats().attack());
            Assertions.assertEquals(expected.skillSnapshots(), player.skillSnapshots());
            Assertions.assertEquals(version, player.skillSnapshots().getFirst().formulaVersion());
            Assertions.assertEquals(4, player.skillSnapshots().getFirst().points());
            Assertions.assertEquals(Position.MID, player.startPosition());
            Assertions.assertEquals(TargetingTactic.WOUNDED_HUNTER, player.targetingTactic());
            throw stopAfterGeneration;
        }).when(generator).generate(Mockito.any(), Mockito.any(), Mockito.anyList(), Mockito.any());

        Assertions.assertSame(stopAfterGeneration, Assertions.assertThrows(IllegalStateException.class,
            () -> processing.process(launched)));
        Assertions.assertEquals(3, item.enhanceLevel(), "Raid must not strip or mutate the equipped enhancement");
        Assertions.assertSame(effects, personage.effects());
        Mockito.verify(loadoutService).resolveCombatGear(List.of(personage), EventType.RAID);
        Mockito.verifyNoMoreInteractions(loadoutService);
        Mockito.verifyNoInteractions(personageService, eventLog);
    }
}
