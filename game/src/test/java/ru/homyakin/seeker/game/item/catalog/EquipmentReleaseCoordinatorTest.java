package ru.homyakin.seeker.game.item.catalog;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.battle.PlayerCombatConfig;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.ItemCatalogService;
import ru.homyakin.seeker.game.item.database.ItemCatalogLifecycleDao;
import ru.homyakin.seeker.game.personage.power.PowerDisplayService;
import ru.homyakin.seeker.game.personage.power.PowerDisplayVersion;

class EquipmentReleaseCoordinatorTest {
    private final EquipmentReleaseConfig config = new EquipmentReleaseConfig();
    private final PlayerCombatConfig combatConfig = new PlayerCombatConfig();
    private final ItemCatalogService catalogService = Mockito.mock(ItemCatalogService.class);
    private final ItemCatalogLifecycleDao lifecycleDao = Mockito.mock(ItemCatalogLifecycleDao.class);

    @Test
    void defaultsOnlyStageTheFirstReleaseWithoutActivation() {
        final var coordinator = coordinator(PowerDisplayVersion.LEGACY_V1);

        coordinator.prepare(List.of(), List.of());

        Assertions.assertEquals(EquipmentCatalogVersion.SCALING_V1, config.catalogVersion());
        Assertions.assertFalse(config.activateOnStartup());
        Mockito.verify(lifecycleDao).getActiveVersions();
        Mockito.verify(catalogService).stageRelease(EquipmentCatalogVersion.SCALING_V1, List.of(), List.of());
        Mockito.verifyNoMoreInteractions(catalogService, lifecycleDao);
    }

    @Test
    void acceptedConfigurationStagesThenActivatesTheReviewedRelease() {
        acceptedConfiguration();
        final var coordinator = coordinator(PowerDisplayVersion.REFERENCE_V1);

        coordinator.prepare(List.of(), List.of());

        final var order = Mockito.inOrder(lifecycleDao, catalogService);
        order.verify(lifecycleDao).getActiveVersions();
        order.verify(catalogService).stageRelease(EquipmentCatalogVersion.SCALING_V5, List.of(), List.of());
        order.verify(catalogService).activateRelease(EquipmentCatalogVersion.SCALING_V5);
        order.verifyNoMoreInteractions();
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = "SCALING_V5", mode = EnumSource.Mode.EXCLUDE)
    void unreviewedReleasesCannotBeActivatedEvenWithOtherCorrectSwitches(EquipmentCatalogVersion version) {
        acceptedConfiguration();
        config.setCatalogVersion(version);

        Assertions.assertThrows(IllegalStateException.class,
            () -> coordinator(PowerDisplayVersion.REFERENCE_V1).prepare(List.of(), List.of()));

        Mockito.verifyNoInteractions(catalogService, lifecycleDao);
    }

    @ParameterizedTest
    @EnumSource(value = SkillFormulaVersion.class, names = "SCALING_SKILLS_V2", mode = EnumSource.Mode.EXCLUDE)
    void mismatchedCombatRulesAreRejectedBeforeAnyDatabaseOperation(SkillFormulaVersion version) {
        acceptedConfiguration();
        combatConfig.setVersion(version);

        Assertions.assertThrows(IllegalStateException.class,
            () -> coordinator(PowerDisplayVersion.REFERENCE_V1).prepare(List.of(), List.of()));

        Mockito.verifyNoInteractions(catalogService, lifecycleDao);
    }

    @Test
    void legacyPowerDisplayIsRejectedBeforeAnyDatabaseOperation() {
        acceptedConfiguration();

        Assertions.assertThrows(IllegalStateException.class,
            () -> coordinator(PowerDisplayVersion.LEGACY_V1).prepare(List.of(), List.of()));

        Mockito.verifyNoInteractions(catalogService, lifecycleDao);
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void stagingTheReviewedCandidateDoesNotActivateIt(EquipmentCatalogVersion version) {
        config.setCatalogVersion(version);

        coordinator(PowerDisplayVersion.LEGACY_V1).prepare(List.of(), List.of());

        Mockito.verify(catalogService).stageRelease(version, List.of(), List.of());
        Mockito.verify(catalogService, Mockito.never()).activateRelease(Mockito.any());
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void staleDefaultConfigurationCannotOverwriteAnAlreadyActiveRelease(EquipmentCatalogVersion version) {
        Mockito.when(lifecycleDao.getActiveVersions()).thenReturn(List.of(version));

        Assertions.assertThrows(IllegalStateException.class,
            () -> coordinator(PowerDisplayVersion.LEGACY_V1).prepare(List.of(), List.of()));

        Mockito.verify(lifecycleDao).getActiveVersions();
        Mockito.verifyNoInteractions(catalogService);
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = "SCALING_V5", mode = EnumSource.Mode.EXCLUDE)
    void anyDifferentActiveReleaseRequiresAnExplicitUpgradeRatherThanSilentReplacement(EquipmentCatalogVersion version) {
        acceptedConfiguration();
        Mockito.when(lifecycleDao.getActiveVersions()).thenReturn(List.of(version));

        Assertions.assertThrows(IllegalStateException.class,
            () -> coordinator(PowerDisplayVersion.REFERENCE_V1).prepare(List.of(), List.of()));

        Mockito.verifyNoInteractions(catalogService);
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void resettingTheActivationFlagDoesNotPermitLegacyCombatOnAnActiveRelease(EquipmentCatalogVersion version) {
        config.setCatalogVersion(version);
        Mockito.when(lifecycleDao.getActiveVersions()).thenReturn(List.of(version));

        Assertions.assertThrows(IllegalStateException.class,
            () -> coordinator(PowerDisplayVersion.REFERENCE_V1).prepare(List.of(), List.of()));

        Mockito.verifyNoInteractions(catalogService);
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void restartingAnActiveReleaseDoesNotPermitLegacyDisplayWithNewCombat(EquipmentCatalogVersion version) {
        acceptedConfiguration();
        config.setCatalogVersion(version);
        config.setActivateOnStartup(false);
        Mockito.when(lifecycleDao.getActiveVersions()).thenReturn(List.of(version));

        Assertions.assertThrows(IllegalStateException.class,
            () -> coordinator(PowerDisplayVersion.LEGACY_V1).prepare(List.of(), List.of()));

        Mockito.verify(lifecycleDao).getActiveVersions();
        Mockito.verifyNoInteractions(catalogService);
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void restartingTheActiveReleaseWithConsistentRulesOnlyRefreshesIt(EquipmentCatalogVersion version) {
        acceptedConfiguration();
        config.setCatalogVersion(version);
        config.setActivateOnStartup(false);
        Mockito.when(lifecycleDao.getActiveVersions()).thenReturn(List.of(version));

        coordinator(PowerDisplayVersion.REFERENCE_V1).prepare(List.of(), List.of());

        Mockito.verify(catalogService).stageRelease(version, List.of(), List.of());
        Mockito.verify(catalogService, Mockito.never()).activateRelease(Mockito.any());
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void selectedCatalogAndNullValidationUseTheSameExplicitConfiguration(EquipmentCatalogVersion version) {
        config.setCatalogVersion(version);
        Assertions.assertEquals(version,
            coordinator(PowerDisplayVersion.LEGACY_V1).selectedCatalog().release().catalogVersion());
        Assertions.assertThrows(IllegalArgumentException.class, () -> config.setCatalogVersion(null));
        Mockito.verifyNoInteractions(catalogService, lifecycleDao);
    }

    private EquipmentReleaseCoordinator coordinator(PowerDisplayVersion displayVersion) {
        return new EquipmentReleaseCoordinator(
            config, combatConfig, new PowerDisplayService(displayVersion), catalogService, lifecycleDao
        );
    }

    private void acceptedConfiguration() {
        config.setCatalogVersion(EquipmentCatalogVersion.SCALING_V5);
        config.setActivateOnStartup(true);
        combatConfig.setVersion(SkillFormulaVersion.SCALING_SKILLS_V2);
    }
}
