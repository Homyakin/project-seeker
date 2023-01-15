package ru.homyakin.seeker.game.experience;

public class ExperienceUtils {

    // Формула здесь - https://github.com/Homyakin/project-seeker/blob/master/documentation/user.md
    public static long getTotalExpToNextLevel(int currentLevel) {
        long neededExpTotal = 0L;
        long neededExpToNextLevel = 0L;
        for (int level = 1; level <= currentLevel; ++level) {
            neededExpToNextLevel += diffBetweenNeededExp(level);
            neededExpTotal += neededExpToNextLevel;
        }
        return neededExpTotal;
    }

    public static int getCurrentLevelForExp(long exp) {
        long neededExpTotal = 0L;
        long neededExpToNextLevel = 0L;
        int level;
        for (level = 1; exp > neededExpTotal; ++level) {
            neededExpToNextLevel += diffBetweenNeededExp(level);
            neededExpTotal += neededExpToNextLevel;
        }
        return level;
    }


    private static long diffBetweenNeededExp(int currentLevel) {
        return (long) (10 * Math.pow(1.15d, currentLevel - 1));
    }
}
