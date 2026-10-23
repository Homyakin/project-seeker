package ru.homyakin.seeker.telegram.command.common.help;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import ru.homyakin.seeker.game.battle.PlayerCombatConfig;
import ru.homyakin.seeker.game.battle.skill.ActiveSkillCatalogProvider;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.catalog.EquipmentReleaseConfig;
import ru.homyakin.seeker.game.item.database.ItemCatalogLifecycleDao;
import ru.homyakin.seeker.game.item.models.ModifierType;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocalizationInitializer;
import ru.homyakin.seeker.locale.battle.BattleLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.user.models.UserId;

class SelectHelpCatalogTest {
    private final EquipmentReleaseConfig config = new EquipmentReleaseConfig();
    private final PlayerCombatConfig combat = new PlayerCombatConfig();
    private final ItemCatalogLifecycleDao lifecycle = Mockito.mock(ItemCatalogLifecycleDao.class);
    private final UserService users = Mockito.mock(UserService.class);
    private final TelegramSender sender = Mockito.mock(TelegramSender.class);
    private final ActiveSkillCatalogProvider catalog = new ActiveSkillCatalogProvider(config, lifecycle);

    @BeforeAll
    static void initLocalization() {
        LocalizationInitializer.initLocale();
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void selectedButInactiveReleaseKeepsElevenSkillsEvenWithSecondCombatVersion(EquipmentCatalogVersion version) {
        config.setCatalogVersion(version);
        combat.setVersion(SkillFormulaVersion.SCALING_SKILLS_V2);
        Mockito.when(lifecycle.getActiveVersions()).thenReturn(List.of());

        final var texts = allPages();

        Assertions.assertEquals(11, texts.split("<b>", -1).length - 1);
        for (final var skill : List.of(ActiveEnum.GUARD, ActiveEnum.PENETRATION, ActiveEnum.ACCUMULATION, ActiveEnum.TEMPO_BREAK)) {
            Assertions.assertFalse(texts.contains("<b>" + BattleLocalization.skillName(Language.RU, skill) + "</b>"), texts);
        }
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void actualActiveReleaseShowsAllFifteenSkillsWithCorrectSlotsAndSecondRules(EquipmentCatalogVersion version) {
        config.setCatalogVersion(version);
        combat.setVersion(SkillFormulaVersion.SCALING_SKILLS_V2);
        Mockito.when(lifecycle.getActiveVersions()).thenReturn(List.of(version));

        final var texts = allPages();

        Assertions.assertEquals(15, texts.split("<b>", -1).length - 1);
        for (final var skill : ActiveEnum.values()) {
            Assertions.assertTrue(texts.contains("<b>" + BattleLocalization.skillName(Language.RU, skill) + "</b>"), texts);
        }
        Assertions.assertTrue(texts.contains("ровно на одну линию позади"), texts);
        Assertions.assertTrue(texts.contains("ещё на две обычные попытки"), texts);
        Assertions.assertTrue(texts.contains("основного типа атаки"), texts);
        Assertions.assertFalse(texts.contains("В начале хода с шансом"), texts);
        Assertions.assertFalse(texts.contains("— Для:"), texts);
        Assertions.assertFalse(texts.contains("${"), texts);

        final var snapshot = catalog.snapshot();
        Assertions.assertEquals(Set.of(PersonageSlot.OFF_HAND, PersonageSlot.BODY), snapshot.slotsFor(ActiveEnum.GUARD));
        Assertions.assertEquals(ModifierType.DEFENSE, snapshot.typeFor(ActiveEnum.GUARD));
        Assertions.assertTrue(snapshot.sortedSkills(Set.of(PersonageSlot.OFF_HAND, PersonageSlot.BODY)).contains(ActiveEnum.GUARD));
        Assertions.assertFalse(snapshot.sortedSkills(Set.of(PersonageSlot.MAIN_HAND)).contains(ActiveEnum.GUARD));
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V1", "SCALING_V3"})
    void earlierActiveReleaseAlsoUsesItsActuallyAvailableFullModifierCatalog(EquipmentCatalogVersion version) {
        config.setCatalogVersion(version);
        Mockito.when(lifecycle.getActiveVersions()).thenReturn(List.of(version));
        Assertions.assertEquals(15, catalog.snapshot().sortedSkills(Set.of()).size());
        Assertions.assertTrue(catalog.snapshot().sortedSkills(Set.of()).contains(ActiveEnum.TEMPO_BREAK));
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentCatalogVersion.class, names = {"SCALING_V4", "SCALING_V5"})
    void anUnselectedActiveReleaseDoesNotUnlockSkillsAndActivationIsNotLatched(EquipmentCatalogVersion version) {
        config.setCatalogVersion(version);
        Mockito.when(lifecycle.getActiveVersions()).thenReturn(
            List.of(EquipmentCatalogVersion.SCALING_V3),
            List.of(version), List.of()
        );
        final var staged = catalog.snapshot();
        final var active = catalog.snapshot();
        final var reverted = catalog.snapshot();
        Assertions.assertEquals(11, staged.sortedSkills(Set.of()).size());
        Assertions.assertEquals(15, active.sortedSkills(Set.of()).size());
        Assertions.assertSame(staged, reverted);
        Assertions.assertThrows(UnsupportedOperationException.class,
            () -> active.slotsBySkill().put(ActiveEnum.GUARD, Set.of()));
        Assertions.assertThrows(UnsupportedOperationException.class,
            () -> active.slotsFor(ActiveEnum.GUARD).clear());
    }

    private String allPages() {
        final var user = new User(UserId.from(1), true, Language.RU, PersonageId.from(1), Optional.empty());
        Mockito.when(users.forceGetFromPrivate(user.id())).thenReturn(user);
        final var executor = new SelectHelpExecutor(users, Mockito.mock(GroupTgService.class), sender, combat, catalog);
        for (int page = 0; page < 3; page++) {
            executor.execute(new SelectHelp(1, 1, true, HelpSection.BATTLE_SKILLS.name(), page, Set.of()));
        }
        final var messages = ArgumentCaptor.forClass(EditMessageText.class);
        Mockito.verify(sender, Mockito.times(3)).send(messages.capture());
        return String.join("\n", messages.getAllValues().stream().map(EditMessageText::getText).toList());
    }
}
