package ru.homyakin.seeker.infrastructure.init;

public enum InitGameDataType {
    TEST("test"),
    PROD("prod"),
    ;

    private final String folder;

    InitGameDataType(String folder) {
        this.folder = folder;
    }

    public String folder() {
        return folder;
    }
}
