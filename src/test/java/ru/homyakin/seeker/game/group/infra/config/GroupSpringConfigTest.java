package ru.homyakin.seeker.game.group.infra.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.homyakin.seeker.game.group.entity.EventInterval;
import ru.homyakin.seeker.game.group.entity.EventIntervals;
import ru.homyakin.seeker.game.models.Money;

import java.util.List;

import static org.mockito.Mockito.*;

class GroupSpringConfigTest {
    private final GroupSpringConfig groupSpringConfig = new GroupSpringConfig();

    @Test
    void registrationPriceIs1000() {
        Assertions.assertEquals(new Money(1000), groupSpringConfig.registrationPrice());
    }
}
