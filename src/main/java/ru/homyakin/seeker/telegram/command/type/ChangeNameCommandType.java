package ru.homyakin.seeker.telegram.command.type;

import ru.homyakin.seeker.locale.personal.ChangeNameResource;
import ru.homyakin.seeker.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum ChangeNameCommandType {
    CONFIRM,
    REPEAT,
    CANCEL,
    ;

    private static final Map<String, ChangeNameCommandType> textToType = new HashMap<>();

    public static Optional<ChangeNameCommandType> getFromString(String text) {
        return Optional.ofNullable(textToType.get(text));
    }

    public static void fillLocaleMap(ChangeNameResource resource) {
        CommonUtils.putIfKeyPresents(textToType, resource.confirmButton(), ChangeNameCommandType.CONFIRM);
        CommonUtils.putIfKeyPresents(textToType, resource.cancelButton(), ChangeNameCommandType.CANCEL);
        CommonUtils.putIfKeyPresents(textToType, resource.repeatButton(), ChangeNameCommandType.REPEAT);
    }
}
