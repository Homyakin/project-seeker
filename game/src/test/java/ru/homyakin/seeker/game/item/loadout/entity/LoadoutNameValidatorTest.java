package ru.homyakin.seeker.game.item.loadout.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.utils.NameError;

class LoadoutNameValidatorTest {
    @Test
    void acceptsValidName() {
        final var result = LoadoutNameValidator.validate("PvP");
        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals("PvP", result.get());
    }

    @Test
    void trimsName() {
        final var result = LoadoutNameValidator.validate("  Raid  ");
        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals("Raid", result.get());
    }

    @Test
    void rejectsEmptyName() {
        final var result = LoadoutNameValidator.validate("   ");
        Assertions.assertTrue(result.isLeft());
        Assertions.assertInstanceOf(LoadoutNameError.InvalidName.class, result.getLeft());
        Assertions.assertInstanceOf(
            NameError.InvalidLength.class,
            ((LoadoutNameError.InvalidName) result.getLeft()).nameError()
        );
    }

    @Test
    void rejectsTooLongName() {
        final var result = LoadoutNameValidator.validate("a".repeat(21));
        Assertions.assertTrue(result.isLeft());
    }

    @Test
    void rejectsInvalidSymbols() {
        final var result = LoadoutNameValidator.validate("bad@name");
        Assertions.assertTrue(result.isLeft());
        Assertions.assertInstanceOf(
            NameError.NotAllowedSymbols.class,
            ((LoadoutNameError.InvalidName) result.getLeft()).nameError()
        );
    }
}
