package ru.homyakin.seeker.game.battle;

import java.util.List;
import ru.homyakin.seeker.game.personage.models.Personage;

public class TwoPersonageTeamsBattle {
    public static Result battle(List<Personage> firstTeam, List<Personage> secondTeam) {
        int firstSumLevel = 0;
        int secondSumLevel = 0;
        for (final var personage: firstTeam) {
            firstSumLevel += personage.level();
        }
        for (final var personage: secondTeam) {
            secondSumLevel += personage.level();
        }

        if (firstSumLevel > secondSumLevel) {
            return new Result.FirstTeamWin();
        } else if (firstSumLevel < secondSumLevel) {
            return new Result.SecondTeamWin();
        } else {
            return new Result.Draw();
        }
    }

    public abstract static sealed class Result {
        public static final class Draw extends Result {
        }

        public static final class FirstTeamWin extends Result {
        }

        public static final class SecondTeamWin extends Result {
        }
    }
}
