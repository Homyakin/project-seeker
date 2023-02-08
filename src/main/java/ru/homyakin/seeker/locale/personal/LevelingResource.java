package ru.homyakin.seeker.locale.personal;

import java.util.Collections;
import ru.homyakin.seeker.telegram.command.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public record LevelingResource(
    String profileLevelUp,
    String notEnoughLevelingPoints,
    String chooseLevelUpCharacteristic,
    String successLevelUp
) {
    public String profileLevelUp() {
        return StringNamedTemplate.format(
            profileLevelUp,
            Collections.singletonMap("level_up_command", CommandType.LEVEL_UP.getText())
        );
    }
}
