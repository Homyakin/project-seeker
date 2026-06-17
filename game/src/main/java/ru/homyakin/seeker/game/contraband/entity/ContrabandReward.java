package ru.homyakin.seeker.game.contraband.entity;

import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.utils.ProbabilityPicker;
import ru.homyakin.seeker.utils.models.IntRange;

import java.util.Map;

public sealed interface ContrabandReward {

    record Gold(IntRange amount) implements ContrabandReward {
        public Gold(int amount) {
            this(new IntRange(amount, amount));
        }
    }

    record Energy(IntRange amount) implements ContrabandReward {
        public Energy(int amount) {
            this(new IntRange(amount, amount));
        }
    }

    record Item(ProbabilityPicker<ItemRarity> rarityPicker) implements ContrabandReward {
        public Item(ItemRarity rarity) {
            this(new ProbabilityPicker<>(Map.of(rarity, 100)));
        }
    }

    record HealthBuff(IntRange percent, IntRange hours) implements ContrabandReward {
        public HealthBuff(int percent, int hours) {
            this(new IntRange(percent, percent), new IntRange(hours, hours));
        }
    }

    record AttackBuff(IntRange percent, IntRange hours) implements ContrabandReward {
        public AttackBuff(int percent, int hours) {
            this(new IntRange(percent, percent), new IntRange(hours, hours));
        }
    }
}
