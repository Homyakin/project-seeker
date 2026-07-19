package ru.homyakin.seeker.telegram.command.type;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import ru.homyakin.seeker.locale.item.ItemResource;
import ru.homyakin.seeker.utils.CommonUtils;

public enum LoadoutNameCommandType {
    CANCEL,
    ;

    private static final Map<String, LoadoutNameCommandType> textToType = new HashMap<>();

    public static Optional<LoadoutNameCommandType> getFromString(String text) {
        return Optional.ofNullable(textToType.get(text));
    }

    public static void fillLocaleMap(ItemResource resource) {
        CommonUtils.putIfKeyPresents(textToType, resource.cancelLoadoutNameButton(), LoadoutNameCommandType.CANCEL);
    }
}
