package ru.homyakin.seeker.locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.event.launched.CurrentEvents;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.models.StormShards;
import ru.homyakin.seeker.game.online.entity.OnlineStreak;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Energy;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.game.top.models.TopPowerPersonagePosition;
import ru.homyakin.seeker.game.top.models.TopPowerPersonageResult;
import ru.homyakin.seeker.locale.battle.BattleLocalization;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.top.TopLocalization;

class PowerPresentationLocalizationTest {
    private static final String RU_NOTE = "Мощь — приблизительный ориентир и не прогнозирует исход боя.";
    private static final String ES_NOTE = "El poder es una referencia aproximada y no predice el resultado del combate.";

    @BeforeAll
    static void initLocalization() {
        LocalizationInitializer.initLocale();
    }

    @Test
    void legacyPowerFormatterKeepsExistingScaleExplicitly() {
        assertEquals(123, LocaleUtils.legacyPowerForDisplay(12_345));
    }

    @Test
    void profileUsesSinglePowerTermAndApproximationNoteInRussianAndSpanish() {
        final var personage = personage();
        final var characteristics = new Characteristics(1_000, 100, 50);

        final var russian = CommonLocalization.fullProfile(
            Language.RU,
            personage,
            new CurrentEvents(List.of()),
            List.of(),
            characteristics,
            12_345
        );
        final var spanish = CommonLocalization.fullProfile(
            Language.ES,
            personage,
            new CurrentEvents(List.of()),
            List.of(),
            characteristics,
            12_345
        );

        assertTrue(russian.contains("Мощь: 123"));
        assertTrue(russian.contains(RU_NOTE));
        assertTrue(spanish.contains("Poder: 123"));
        assertTrue(spanish.contains(ES_NOTE));
    }

    @Test
    void battleStatsUsesPowerTermAndApproximationNoteInRussianAndSpanish() {
        final var battlePersonage = battlePersonage();

        final var russian = BattleLocalization.battleStats(Language.RU, battlePersonage, List.of());
        final var spanish = BattleLocalization.battleStats(Language.ES, battlePersonage, List.of());

        assertTrue(russian.contains("Мощь:"));
        assertFalse(russian.contains("Сила:"));
        assertTrue(russian.contains(RU_NOTE));
        assertTrue(spanish.contains("Poder:"));
        assertTrue(spanish.contains(ES_NOTE));
    }

    @Test
    void powerRatingIncludesApproximationNoteInRussianAndSpanish() {
        final var id = PersonageId.from(1);
        final var result = new TopPowerPersonageResult(List.of(new TopPowerPersonagePosition(
            id,
            "Искатель",
            BadgeView.STANDARD,
            Optional.empty(),
            12_345
        )));

        final var russian = TopLocalization.topPowerPersonageGroup(Language.RU, id, result);
        final var spanish = TopLocalization.topPowerPersonageGroup(Language.ES, id, result);

        assertTrue(russian.contains(RU_NOTE));
        assertTrue(russian.contains(": 123"));
        assertTrue(spanish.contains(ES_NOTE));
        assertTrue(spanish.contains(": 123"));
    }

    @Test
    void battleReportPowerSlotAcceptsFinalPreformattedScaleWithoutExtraConversion() {
        final var russian = CommonLocalization.battleReportPower(Language.RU, "1000");
        final var spanish = CommonLocalization.battleReportPower(Language.ES, "1000");

        assertTrue(russian.contains("Мощь: 1000"));
        assertTrue(russian.contains(RU_NOTE));
        assertTrue(spanish.contains("Poder: 1000"));
        assertTrue(spanish.contains(ES_NOTE));
    }

    private static Personage personage() {
        final var now = LocalDateTime.now();
        return new Personage(
            PersonageId.from(1),
            "Искатель",
            Optional.empty(),
            Optional.empty(),
            Money.ZERO,
            StormShards.ZERO,
            new Energy(100, now, Duration.ofHours(1)),
            BadgeView.STANDARD,
            PersonageEffects.EMPTY,
            Position.FRONT,
            ru.homyakin.seeker.game.battle.targeting.TargetingTactic.THREAT,
            new OnlineStreak(1, now)
        );
    }

    private static BattlePersonage battlePersonage() {
        return new BattlePersonage(
            List.of(
                Item.weapon(
                    AttackType.SLASH,
                    1,
                    100,
                    new Modifier(ActiveEnum.DOUBLE_ATTACK),
                    ItemRarity.COMMON
                ),
                Item.armor(
                    DefenseType.CLOTH,
                    20,
                    1_000,
                    new Modifier(ActiveEnum.THORNS),
                    ItemRarity.COMMON
                ),
                Item.stats(10, 5, 0.5, 100, 10)
            ),
            Position.FRONT,
            Map.of()
        );
    }
}
