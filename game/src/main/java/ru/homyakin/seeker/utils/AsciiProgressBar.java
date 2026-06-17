package ru.homyakin.seeker.utils;

public final class AsciiProgressBar {
    public static final int DEFAULT_WIDTH = 12;

    private AsciiProgressBar() {
    }

    /**
     * ASCII bar: {@code #} filled, {@code -} empty, length {@code width}.
     */
    public static String bar(int done, int total, int width) {
        if (width <= 0) {
            return "";
        }
        if (total <= 0) {
            return "-".repeat(width);
        }
        final int filled = (int) Math.round((double) width * Math.min(1.0, (double) done / total));
        final int f = Math.min(width, Math.max(0, filled));
        return "#".repeat(f) + "-".repeat(width - f);
    }

    public static int percent100(int done, int total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.min(100, (100L * done) / total);
    }

    public static String bracketedBar(int done, int total, int width) {
        return "[" + bar(done, total, width) + "]";
    }
}
