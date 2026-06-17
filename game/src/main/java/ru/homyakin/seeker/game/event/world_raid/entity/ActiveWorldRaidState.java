package ru.homyakin.seeker.game.event.world_raid.entity;

public sealed interface ActiveWorldRaidState {

    record Research(
        int contribution,
        int requiredContribution
    ) implements ActiveWorldRaidState {
        public double progressInPercent() {
            if (requiredContribution == 0) {
                return 100;
            }
            return (double) contribution / requiredContribution * 100;
        }

        public boolean isInProgress() {
            return !isCompleted();
        }

        public boolean isCompleted() {
            return contribution >= requiredContribution;
        }
    }

    record Battle(long launchedEventId) implements ActiveWorldRaidState {
    }
}
