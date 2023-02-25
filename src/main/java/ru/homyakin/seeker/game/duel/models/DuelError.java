package ru.homyakin.seeker.game.duel.models;

import ru.homyakin.seeker.game.models.Money;

public abstract sealed class DuelError {
    public static final class PersonageAlreadyHasDuel extends DuelError {
    }

    public static final class InitiatingPersonageNotEnoughMoney extends DuelError {
        private final Money money;

        public InitiatingPersonageNotEnoughMoney(Money money) {
            this.money = money;
        }

        public Money money() {
            return money;
        }
    }

    public static final class AcceptingPersonageNotEnoughMoney extends DuelError {
        private final Money money;

        public AcceptingPersonageNotEnoughMoney(Money money) {
            this.money = money;
        }

        public Money money() {
            return money;
        }
    }
}

