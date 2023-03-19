package ru.homyakin.seeker.telegram.command.common.help;

public enum HelpSection {
    RAIDS,
    DUELS,
    MENU,
    PERSONAGE,
    INFO,
    ;

    public static HelpSection findForce(String section) {
        for (HelpSection helpSection : HelpSection.values()) {
            if (helpSection.name().equals(section)) {
                return helpSection;
            }
        }
        throw new IllegalStateException("Unknown help section " + section);
    }
}
