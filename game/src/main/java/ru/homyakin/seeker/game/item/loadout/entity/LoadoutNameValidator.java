package ru.homyakin.seeker.game.item.loadout.entity;

import io.vavr.control.Either;
import java.util.regex.Pattern;
import ru.homyakin.seeker.game.utils.NameError;

public final class LoadoutNameValidator {
    private LoadoutNameValidator() {
    }

    public static Either<LoadoutNameError, String> validate(String name) {
        final var trimmed = name == null ? "" : name.trim();
        if (trimmed.length() < MIN_NAME_LENGTH || trimmed.length() > MAX_NAME_LENGTH) {
            return Either.left(new LoadoutNameError.InvalidName(
                new NameError.InvalidLength(MIN_NAME_LENGTH, MAX_NAME_LENGTH)
            ));
        }
        if (!NAME_PATTERN.matcher(trimmed).matches()) {
            return Either.left(new LoadoutNameError.InvalidName(new NameError.NotAllowedSymbols()));
        }
        return Either.right(trimmed);
    }

    private static final int MIN_NAME_LENGTH = 1;
    private static final int MAX_NAME_LENGTH = 20;

    private static final String CYRILLIC = "а-яА-ЯёЁ";
    private static final String ENGLISH = "a-zA-Z";
    private static final String SPANISH = "áéíóúÁÉÍÓÚñÑüÜ";
    private static final String NUMBERS = "0-9";
    private static final String SPECIAL = "_\\-\\.#№ ";
    private static final Pattern NAME_PATTERN = Pattern.compile(
        "[" + CYRILLIC + ENGLISH + SPANISH + NUMBERS + SPECIAL + "]+"
    );
}
