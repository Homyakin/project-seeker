package ru.homyakin.seeker.game.item.models;

import java.util.List;
import java.util.Optional;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.command.type.CommandType;

public record Item(
    long id,
    ItemObject object,
    List<Modifier> modifiers,
    Optional<PersonageId> personageId,
    boolean isEquipped,
    Characteristics characteristics
) {
    /**
     * @param requestedLanguage запрашиваемый язык
     * @return Возвращает requestedLanguage если он присутствует во object и во всех modifiers,
     *         иначе название предмета будет на разных языках, что может нарушить смысл и порядок слов
     */
    public Language getItemLanguage(Language requestedLanguage) {
        if (requestedLanguage == Language.DEFAULT) {
            return Language.DEFAULT;
        }

        if (!object.locales().containsKey(requestedLanguage)) {
            return Language.DEFAULT;
        }

        for (final var modifier: modifiers) {
            if (!modifier.locales().containsKey(requestedLanguage)) {
                return Language.DEFAULT;
            }
        }

        return requestedLanguage;
    }

    public String putOnCommand() {
        return CommandType.PUT_ON.getText() + TextConstants.TG_COMMAND_DELIMITER + id;
    }

    public String takeOffCommand() {
        return CommandType.TAKE_OFF.getText() + TextConstants.TG_COMMAND_DELIMITER + id;
    }

    public String confirmDropCommand() {
        return CommandType.CONFIRM_DROP_ITEM.getText() + TextConstants.CALLBACK_DELIMITER + id;
    }
}
