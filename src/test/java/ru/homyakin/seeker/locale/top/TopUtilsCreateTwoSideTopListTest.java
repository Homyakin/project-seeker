package ru.homyakin.seeker.locale.top;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.season.entity.SeasonNumber;
import ru.homyakin.seeker.game.top.models.TopDonatePosition;
import ru.homyakin.seeker.game.top.models.TopDonateResult;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocalizationInitializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TopUtilsCreateTwoSideTopListTest {
    @BeforeAll
    public static void init() {
        LocalizationInitializer.initLocale();
    }

    @Test
    public void When_EmptyList_Then_ReturnEmptyString() {
        final var topDonatePositions = new ArrayList<TopDonatePosition>();
        final var top = new TopDonateResult(topDonatePositions, SeasonNumber.of(1));
        final var result = TopUtils.createTwoSideTopList(Language.RU, PersonageId.from(0L), top);
        Assertions.assertEquals("", result);
    }

    @Test
    public void When_ListWithLessThan5Positions_Then_ReturnAllPositions() {
        final var topDonatePositions = generateDonatePositions(3);
        final var top = new TopDonateResult(topDonatePositions, SeasonNumber.of(1));
        final var result = TopUtils.createTwoSideTopList(Language.RU, PersonageId.from(1L), top);
        Assertions.assertEquals(
            """
                1. 🔰Тест1: 💰1000
                2. 🔰Тест2: 💰900
                3. 🔰Тест3: 💰800""",
            result
        );
    }

    @Test
    public void When_ListWith6Positions_Then_ReturnAll6Positions() {
        final var topDonatePositions = generateDonatePositions(6);
        final var top = new TopDonateResult(topDonatePositions, SeasonNumber.of(1));
        final var result = TopUtils.createTwoSideTopList(Language.RU, PersonageId.from(999L), top);
        Assertions.assertEquals(
            """
                1. 🔰Тест1: 💰1000
                2. 🔰Тест2: 💰900
                3. 🔰Тест3: 💰800
                4. 🔰Тест4: 💰700
                5. 🔰Тест5: 💰600
                6. 🔰Тест6: 💰500""",
            result
        );
    }

    @Test
    public void When_ListWith10Positions_Then_ReturnAll10Positions() {
        final var topDonatePositions = generateDonatePositions(10);
        final var top = new TopDonateResult(topDonatePositions, SeasonNumber.of(1));
        final var result = TopUtils.createTwoSideTopList(Language.RU, PersonageId.from(6), top);
        Assertions.assertEquals(
            """
                1. 🔰Тест1: 💰1000
                2. 🔰Тест2: 💰900
                3. 🔰Тест3: 💰800
                4. 🔰Тест4: 💰700
                5. 🔰Тест5: 💰600
                6. 🔰Тест6: 💰500
                7. 🔰Тест7: 💰400
                8. 🔰Тест8: 💰300
                9. 🔰Тест9: 💰200
                10. 🔰Тест10: 💰100""",
            result
        );
    }

    @Test
    public void When_RequestedPersonageInTop5_Then_ReturnOnlyTop5AndBottom5() {
        final var topDonatePositions = generateDonatePositions(15);
        final var top = new TopDonateResult(topDonatePositions, SeasonNumber.of(1));
        final var result = TopUtils.createTwoSideTopList(Language.RU, PersonageId.from(2L), top);
        Assertions.assertEquals(
            """
                1. 🔰Тест1: 💰1000
                2. 🔰Тест2: 💰900
                3. 🔰Тест3: 💰800
                4. 🔰Тест4: 💰700
                5. 🔰Тест5: 💰600
                -----------
                11. 🔰Тест11: 💰0
                12. 🔰Тест12: 💰-100
                13. 🔰Тест13: 💰-200
                14. 🔰Тест14: 💰-300
                15. 🔰Тест15: 💰-400""",
            result
        );
    }

    @Test
    public void When_RequestedPersonageInBottom5_Then_ReturnTop5AndBottom5() {
        final var topDonatePositions = generateDonatePositions(15);
        final var top = new TopDonateResult(topDonatePositions, SeasonNumber.of(1));
        final var result = TopUtils.createTwoSideTopList(Language.RU, PersonageId.from(13L), top);
        Assertions.assertEquals(
            """
                1. 🔰Тест1: 💰1000
                2. 🔰Тест2: 💰900
                3. 🔰Тест3: 💰800
                4. 🔰Тест4: 💰700
                5. 🔰Тест5: 💰600
                -----------
                11. 🔰Тест11: 💰0
                12. 🔰Тест12: 💰-100
                13. 🔰Тест13: 💰-200
                14. 🔰Тест14: 💰-300
                15. 🔰Тест15: 💰-400""",
            result
        );
    }

    @Test
    public void When_RequestedPersonageInMiddleSection_Then_ShowInMiddleSection() {
        final var topDonatePositions = generateDonatePositions(15);
        final var top = new TopDonateResult(topDonatePositions, SeasonNumber.of(1));
        final var result = TopUtils.createTwoSideTopList(Language.RU, PersonageId.from(8L), top);
        Assertions.assertEquals(
            """
                1. 🔰Тест1: 💰1000
                2. 🔰Тест2: 💰900
                3. 🔰Тест3: 💰800
                4. 🔰Тест4: 💰700
                5. 🔰Тест5: 💰600
                -----------
                8. 🔰Тест8: 💰300
                -----------
                11. 🔰Тест11: 💰0
                12. 🔰Тест12: 💰-100
                13. 🔰Тест13: 💰-200
                14. 🔰Тест14: 💰-300
                15. 🔰Тест15: 💰-400""",
            result
        );
    }

    @Test
    public void When_RequestedPersonageAdjacentToBottom_Then_ShowInBottomSection() {
        final var topDonatePositions = generateDonatePositions(15);
        final var top = new TopDonateResult(topDonatePositions, SeasonNumber.of(1));
        final var result = TopUtils.createTwoSideTopList(Language.RU, PersonageId.from(10L), top);
        Assertions.assertEquals(
            """
                1. 🔰Тест1: 💰1000
                2. 🔰Тест2: 💰900
                3. 🔰Тест3: 💰800
                4. 🔰Тест4: 💰700
                5. 🔰Тест5: 💰600
                -----------
                10. 🔰Тест10: 💰100
                11. 🔰Тест11: 💰0
                12. 🔰Тест12: 💰-100
                13. 🔰Тест13: 💰-200
                14. 🔰Тест14: 💰-300
                15. 🔰Тест15: 💰-400""",
            result
        );
    }

    @Test
    public void When_RequestedPersonageAdjacentToTop_Then_ShowInTopSection() {
        final var topDonatePositions = generateDonatePositions(15);
        final var top = new TopDonateResult(topDonatePositions, SeasonNumber.of(1));
        final var result = TopUtils.createTwoSideTopList(Language.RU, PersonageId.from(6L), top);
        Assertions.assertEquals(
            """
                1. 🔰Тест1: 💰1000
                2. 🔰Тест2: 💰900
                3. 🔰Тест3: 💰800
                4. 🔰Тест4: 💰700
                5. 🔰Тест5: 💰600
                6. 🔰Тест6: 💰500
                -----------
                11. 🔰Тест11: 💰0
                12. 🔰Тест12: 💰-100
                13. 🔰Тест13: 💰-200
                14. 🔰Тест14: 💰-300
                15. 🔰Тест15: 💰-400""",
            result
        );
    }

    @Test
    public void When_11PositionsAndRequestedIs6_Then_ShowAllPositionsWithoutSeparator() {
        final var topDonatePositions = generateDonatePositions(11);
        final var top = new TopDonateResult(topDonatePositions, SeasonNumber.of(1));
        final var result = TopUtils.createTwoSideTopList(Language.RU, PersonageId.from(6L), top);
        Assertions.assertEquals(
            """
                1. 🔰Тест1: 💰1000
                2. 🔰Тест2: 💰900
                3. 🔰Тест3: 💰800
                4. 🔰Тест4: 💰700
                5. 🔰Тест5: 💰600
                6. 🔰Тест6: 💰500
                7. 🔰Тест7: 💰400
                8. 🔰Тест8: 💰300
                9. 🔰Тест9: 💰200
                10. 🔰Тест10: 💰100
                11. 🔰Тест11: 💰0""",
            result
        );
    }

    @Test
    public void When_RequestedPersonageNotPresent_Then_ReturnTop5AndBottom5() {
        final var topDonatePositions = generateDonatePositions(15);
        final var top = new TopDonateResult(topDonatePositions, SeasonNumber.of(1));
        final var result = TopUtils.createTwoSideTopList(Language.RU, PersonageId.from(999L), top);
        Assertions.assertEquals(
            """
                1. 🔰Тест1: 💰1000
                2. 🔰Тест2: 💰900
                3. 🔰Тест3: 💰800
                4. 🔰Тест4: 💰700
                5. 🔰Тест5: 💰600
                -----------
                11. 🔰Тест11: 💰0
                12. 🔰Тест12: 💰-100
                13. 🔰Тест13: 💰-200
                14. 🔰Тест14: 💰-300
                15. 🔰Тест15: 💰-400""",
            result
        );
    }

    private List<TopDonatePosition> generateDonatePositions(int count) {
        final var list = new ArrayList<TopDonatePosition>();
        for (int i = 1; i <= count; ++i) {
            long donateMoney = 1000L - ((i - 1) * 100L);
            list.add(
                new TopDonatePosition(
                    PersonageId.from(i),
                    "Тест" + i,
                    BadgeView.STANDARD,
                    Optional.empty(),
                    donateMoney
                )
            );
        }
        return list;
    }
}
