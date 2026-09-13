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
    boolean isEquipped,
    int enhanceLevel,
    long enhanceRevision
) {
    public PersonageItem(
        long id,
        int objectId,
        ItemObject object,
        Optional<Integer> modifierId,
        Optional<Modifier> modifier,
        ItemRarity rarity,
        Optional<PersonageId> personageId,
        boolean isEquipped,
        int enhanceLevel
    ) {
        this(id, objectId, object, modifierId, modifier, rarity, personageId, isEquipped, enhanceLevel, 0);
    }

    public PersonageItem {
        if (enhanceLevel < 0) {
            throw new IllegalArgumentException("Enhance level must be non-negative: " + enhanceLevel);
        }
        if (enhanceRevision < 0) {
            throw new IllegalArgumentException("Enhance revision must be non-negative: " + enhanceRevision);
        }
    }

    public Item toItem() {
        return new Item(object, modifier, rarity, enhanceLevel);
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
