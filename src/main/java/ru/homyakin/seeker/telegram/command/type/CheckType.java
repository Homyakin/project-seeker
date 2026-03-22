package ru.homyakin.seeker.telegram.command.type;

enum CheckType {
    EQUALS,
    STARTS_WITH,
    MAP,
    /**
     * Аргумент после <code>/start</code> в личке; сопоставляется через {@link CommandType#getFromStartArgument}.
     */
    START_PAYLOAD,
    ;
}
