package ru.homyakin.seeker.game.personage.models.errors;

public abstract sealed class NameError {
    public static final class InvalidLength extends NameError {
        private final int minLength;
        private final int maxLength;

        public InvalidLength(int minLength, int maxLength) {
            this.minLength = minLength;
            this.maxLength = maxLength;
        }

        public int minLength() {
            return minLength;
        }

        public int maxLength() {
            return maxLength;
        }
    }

    public static final class NotAllowedSymbols extends NameError {
    }
}
