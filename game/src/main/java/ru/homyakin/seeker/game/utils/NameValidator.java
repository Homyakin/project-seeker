package ru.homyakin.seeker.game.utils;

import io.vavr.control.Either;

import java.util.regex.Pattern;

public class NameValidator {
    public static Either<NameError, String> validateName(String name) {
        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            return Either.left(new NameError.InvalidLength(MIN_NAME_LENGTH, MAX_NAME_LENGTH));
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            return Either.left(new NameError.NotAllowedSymbols());
        }
        return Either.right(name);
    }

    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 25;

    private static final String CYRILLIC = "а-яА-ЯёЁ";
    private static final String ENGLISH = "a-zA-Z";
    private static final String SPANISH = "áéíóúÁÉÍÓÚñÑüÜ";
    private static final String NUMBERS = "0-9";
    private static final String SPECIAL = "_\\-\\.#№ ";
    private static final Pattern NAME_PATTERN = Pattern.compile(
        "[" + CYRILLIC + ENGLISH + SPANISH + NUMBERS + SPECIAL + "]+"
    );
}
