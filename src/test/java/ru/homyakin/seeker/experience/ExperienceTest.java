package ru.homyakin.seeker.experience;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.experience.ExperienceUtils;

// Цифры отсюда https://github.com/Homyakin/project-seeker/blob/master/documentation/exp.md
public class ExperienceTest {

    @Test
    @DisplayName("Experience to level 2 equals 10")
    public void neededExpToLevel2() {
        Assertions.assertEquals(10L, ExperienceUtils.getTotalExpToNextLevel(1));
    }

    @Test
    @DisplayName("Experience to level 25 equals 14523")
    public void neededExpToLevel25() {
        Assertions.assertEquals(12406L, ExperienceUtils.getTotalExpToNextLevel(24));
    }

    @Test
    @DisplayName("Experience to level 50 equals 477313")
    public void neededExpToLevel50() {
        Assertions.assertEquals(477313L, ExperienceUtils.getTotalExpToNextLevel(49));
    }

    @Test
    public void t() {
        Long l = Long.MAX_VALUE;
        System.out.println();
    }
}
