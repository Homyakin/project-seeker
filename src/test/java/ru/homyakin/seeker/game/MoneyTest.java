package ru.homyakin.seeker.game;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.utils.RandomUtils;

public class MoneyTest {
    @Test
    @DisplayName("Adding to small int working correct")
    public void simpleAdd() {
        int initValue = RandomUtils.getInInterval(0, 100);
        Money addMoney = new Money(RandomUtils.getInInterval(0, 100));
        final var money = new Money(initValue);

        Assertions.assertEquals(money.add(addMoney).value(), initValue + addMoney.value());
    }

    @Test
    @DisplayName("If adding more than big int, than value is big int")
    public void tooBigAdd() {
        final var money = new Money(100);

        Assertions.assertEquals(money.add(new Money(Integer.MAX_VALUE)).value(), Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("Money can't be less than zero")
    public void tooSmallAdd() {
        final var money = new Money(0);
        Assertions.assertThrows(IllegalStateException.class, () -> money.add(new Money(-1)));
    }
}
