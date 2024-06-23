package ru.homyakin.seeker.utils;

public class MathUtils {
    public static int doubleToIntWithMinMaxValues(double d) {
        if (d > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else if (d < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        } else {
            return (int) d;
        }
    }

    public static double log(double base, double value) {
        return Math.log(value) / Math.log(base);
    }

    /**
     * Описывает функцию вида 1/x => MAX(multiplier / (x + offset) + max, 0)
     * @param x - переменная функции
     * @param multiplier - влияет на растянутость графика
     * @param offset - смещение графика по оси переменной
     * @param max - число к которому стремится функция
     */
    public static double calcOneDivideXFunc(double x, double multiplier, double offset, double max) {
        if (x > offset) {
            return Math.max(multiplier / (x - offset) + max, 0);
        }
        return 0;
    }
}
