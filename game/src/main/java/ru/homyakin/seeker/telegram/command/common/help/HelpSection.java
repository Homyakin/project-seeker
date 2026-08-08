package ru.homyakin.seeker.telegram.command.common.help;

public enum HelpSection {
    MAIN,
    RAIDS,
    DUELS,
    MENU,
    PERSONAGE,
    INFO,
    BATTLE_SYSTEM,
    BATTLE_GENERAL,
    BATTLE_MATRIX,
    BATTLE_SKILLS,
    SEASONS,
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
