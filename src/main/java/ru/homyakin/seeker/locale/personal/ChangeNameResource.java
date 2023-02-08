package ru.homyakin.seeker.locale.personal;

import java.util.Collections;
import java.util.HashMap;
import ru.homyakin.seeker.telegram.command.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public record ChangeNameResource(
    String changeNameWithoutName,
    String personageNameInvalidLength,
    String personageNameInvalidSymbols,
    String successNameChange
) {
    public String changeNameWithoutName() {
        return StringNamedTemplate.format(
            changeNameWithoutName,
            Collections.singletonMap("name_command", CommandType.CHANGE_NAME.getText())
        );
    }
    public String personageNameInvalidLength(int minNameLength, int maxNameLength) {
        final var params = new HashMap<String, Object>() {{
            put("max_name_length", maxNameLength);
            put("min_name_length", minNameLength);
        }};
        return StringNamedTemplate.format(
            personageNameInvalidLength,
            params
        );
    }
}
