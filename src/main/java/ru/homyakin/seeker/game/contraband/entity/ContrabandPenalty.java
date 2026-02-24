package ru.homyakin.seeker.game.contraband.entity;

import ru.homyakin.seeker.utils.models.IntRange;

public sealed interface ContrabandPenalty {

    record HealthDebuff(IntRange percent, IntRange hours) implements ContrabandPenalty {
        public HealthDebuff(int percent, int hours) {
            this(new IntRange(percent, percent), new IntRange(hours, hours));
        }
    }

    record AttackDebuff(IntRange percent, IntRange hours) implements ContrabandPenalty {
        public AttackDebuff(int percent, int hours) {
            this(new IntRange(percent, percent), new IntRange(hours, hours));
        }
    }

    record GoldLoss(IntRange goldAmount) implements ContrabandPenalty {
        public GoldLoss(int goldAmount) {
            this(new IntRange(goldAmount, goldAmount));
        }
    }

    record Nothing() implements ContrabandPenalty {
        public static final Nothing INSTANCE = new Nothing();
    }
}
