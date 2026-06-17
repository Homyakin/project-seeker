package ru.homyakin.seeker.game.item.models;

import java.util.Optional;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.command.type.CommandType;

public record PersonageItem(
    long id,
    int objectId,
    ItemObject object,
    Optional<Integer> modifierId,
    Optional<Modifier> modifier,
    ItemRarity rarity,
    Optional<PersonageId> personageId,
    boolean isEquipped
) {
    public Item toItem() {
        return new Item(object, modifier, rarity);
    }

    public Language getItemLanguage(Language requestedLanguage) {
        if (requestedLanguage == Language.DEFAULT) {
            return Language.DEFAULT;
        }
        if (!object.locales().containsKey(requestedLanguage)) {
            return Language.DEFAULT;
        }
        if (modifier.isPresent() && !modifier.get().locales().containsKey(requestedLanguage)) {
            return Language.DEFAULT;
        }
        return requestedLanguage;
    }

    public String putOnCommand() {
        return CommandType.PUT_ON.getText() + TextConstants.TG_COMMAND_DELIMITER + id;
    }

    public String takeOffCommand() {
        return CommandType.TAKE_OFF.getText() + TextConstants.TG_COMMAND_DELIMITER + id;
    }
}
