package ru.homyakin.seeker.locale.top;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.personage.badge.BadgeView;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.top.models.TopRaidPosition;
import ru.homyakin.seeker.game.top.models.TopRaidResult;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocalizationInitializer;

public class TopLocalizationTopRaidWeekTest {
    @BeforeAll
    public static void init() {
        LocalizationInitializer.initLocale();
    }

    @Test
    public void When_RequestedPersonageInTop10_And_PersonageCountLessThan10_Then_ReturnAllPersonages() {
        // given
        final var topRaidPositions = generatePositions(3);
        final var top = new TopRaidResult(
            LocalDate.of(2020, 11, 11),
            LocalDate.of(2020, 11, 12),
            topRaidPositions
        );

        //when
        final var result = TopLocalization.topRaidWeek(Language.RU, PersonageId.from(0L), top);

        //then
        Assertions.assertEquals(
            """
                🏆Топ всех Искателей в рейдах за неделю <code>2020-11-11 - 2020-11-12</code>:
                                
                1. 🔰Тест1 3/3
                2. 🔰Тест2 2/2
                3. 🔰Тест3 1/1
                
                Всего участников: 3""",
            result
        );
    }

    @Test
    public void When_RequestedPersonageNotPresent_And_PersonageCountMoreThan10_Then_Return10Personages() {
        // given
        final var topRaidPositions = generatePositions(11);
        final var top = new TopRaidResult(
            LocalDate.of(2021, 11, 11),
            LocalDate.of(2021, 11, 12),
            topRaidPositions
        );

        //when
        final var result = TopLocalization.topRaidWeek(Language.RU, PersonageId.from(1000L), top);

        //then
        Assertions.assertEquals(
            """
                🏆Топ всех Искателей в рейдах за неделю <code>2021-11-11 - 2021-11-12</code>:
                                
                1. 🔰Тест1 11/11
                2. 🔰Тест2 10/10
                3. 🔰Тест3 9/9
                4. 🔰Тест4 8/8
                5. 🔰Тест5 7/7
                6. 🔰Тест6 6/6
                7. 🔰Тест7 5/5
                8. 🔰Тест8 4/4
                9. 🔰Тест9 3/3
                10. 🔰Тест10 2/2
                
                Всего участников: 11""",
            result
        );
    }

    @Test
    public void When_RequestedPersonageHas11Position_And_PersonageCountMoreThan11_Then_Return11Personages() {
        // given
        final var topRaidPositions = generatePositions(13);
        final var top = new TopRaidResult(
            LocalDate.of(2021, 11, 11),
            LocalDate.of(2021, 11, 12),
            topRaidPositions
        );

        //when
        final var result = TopLocalization.topRaidWeek(Language.RU, PersonageId.from(10L), top);

        //then
        Assertions.assertEquals(
            """
                🏆Топ всех Искателей в рейдах за неделю <code>2021-11-11 - 2021-11-12</code>:
                                
                1. 🔰Тест1 13/13
                2. 🔰Тест2 12/12
                3. 🔰Тест3 11/11
                4. 🔰Тест4 10/10
                5. 🔰Тест5 9/9
                6. 🔰Тест6 8/8
                7. 🔰Тест7 7/7
                8. 🔰Тест8 6/6
                9. 🔰Тест9 5/5
                10. 🔰Тест10 4/4
                11. 🔰Тест11 3/3
                
                Всего участников: 13""",
            result
        );
    }

    @Test
    public void When_RequestedPersonageHasLastPosition_And_PersonageCountMoreThan11_Then_Return10Personages_And_2AtTheEnd() {
        // given
        final var topRaidPositions = generatePositions(15);
        final var top = new TopRaidResult(
            LocalDate.of(2021, 11, 11),
            LocalDate.of(2021, 11, 12),
            topRaidPositions
        );

        //when
        final var result = TopLocalization.topRaidWeek(Language.RU, PersonageId.from(14L), top);

        //then
        Assertions.assertEquals(
            """
                🏆Топ всех Искателей в рейдах за неделю <code>2021-11-11 - 2021-11-12</code>:
                                
                1. 🔰Тест1 15/15
                2. 🔰Тест2 14/14
                3. 🔰Тест3 13/13
                4. 🔰Тест4 12/12
                5. 🔰Тест5 11/11
                6. 🔰Тест6 10/10
                7. 🔰Тест7 9/9
                8. 🔰Тест8 8/8
                9. 🔰Тест9 7/7
                10. 🔰Тест10 6/6
                -----------
                14. 🔰Тест14 2/2
                15. 🔰Тест15 1/1
                
                Всего участников: 15""",
            result
        );
    }

    @Test
    public void When_RequestedPersonageHasNotLastPosition_And_PersonageCountMoreThan12_Then_Return10Personages_And_3AtTheEnd() {
        // given
        final var topRaidPositions = generatePositions(16);
        final var top = new TopRaidResult(
            LocalDate.of(2021, 11, 11),
            LocalDate.of(2021, 11, 12),
            topRaidPositions
        );

        //when
        final var result = TopLocalization.topRaidWeek(Language.RU, PersonageId.from(14L), top);

        //then
        Assertions.assertEquals(
            """
                🏆Топ всех Искателей в рейдах за неделю <code>2021-11-11 - 2021-11-12</code>:
                                
                1. 🔰Тест1 16/16
                2. 🔰Тест2 15/15
                3. 🔰Тест3 14/14
                4. 🔰Тест4 13/13
                5. 🔰Тест5 12/12
                6. 🔰Тест6 11/11
                7. 🔰Тест7 10/10
                8. 🔰Тест8 9/9
                9. 🔰Тест9 8/8
                10. 🔰Тест10 7/7
                -----------
                14. 🔰Тест14 3/3
                15. 🔰Тест15 2/2
                16. 🔰Тест16 1/1
                
                Всего участников: 16""",
            result
        );
    }

    private List<TopRaidPosition> generatePositions(int count) {
        final var list = new ArrayList<TopRaidPosition>();
        for (int i = 0; i < count; ++i) {
            list.add(
                new TopRaidPosition(
                    PersonageId.from(i),
                    "Тест" + (i + 1),
                    BadgeView.STANDARD,
                    count - i,
                    0
                )
            );
        }
        return list;
    }
}
