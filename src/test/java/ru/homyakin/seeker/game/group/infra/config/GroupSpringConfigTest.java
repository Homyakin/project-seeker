package ru.homyakin.seeker.game.group.infra.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.models.Money;

class GroupSpringConfigTest {
    private final GroupSpringConfig groupSpringConfig = new GroupSpringConfig();

    @Test
    void registrationPriceIs1000() {
        Assertions.assertEquals(new Money(500), groupSpringConfig.registrationPrice());
    }
}
