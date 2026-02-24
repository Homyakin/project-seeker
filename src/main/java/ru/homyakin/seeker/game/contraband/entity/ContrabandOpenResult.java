package ru.homyakin.seeker.game.contraband.entity;

import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;

public sealed interface ContrabandOpenResult {

    sealed interface Success extends ContrabandOpenResult {

        record Gold(Money amount) implements Success {}

        record ItemReward(Item item) implements Success {}

        record Energy(int amount) implements Success {}

        record Buff(PersonageEffect effect) implements Success {}
    }

    sealed interface Failure extends ContrabandOpenResult {

        record Debuff(PersonageEffect effect) implements Failure {}

        record GoldLoss(int amount) implements Failure {}

        enum Nothing implements Failure {
            INSTANCE
        }
    }
}
