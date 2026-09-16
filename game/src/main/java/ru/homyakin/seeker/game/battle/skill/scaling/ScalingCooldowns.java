package ru.homyakin.seeker.game.battle.skill.scaling;

import java.util.Optional;

public final class ScalingCooldowns {
    private ScalingCooldowns() {
    }

    public static Optional<CooldownSchedule> hitAndRun(int points) {
        return switch (ScalingSkillMath.effectivePoints(points)) {
            case 0 -> Optional.empty();
            case 1 -> fixed(6);
            case 2 -> fixed(5);
            case 3 -> alternating(4, 5);
            case 4 -> fixed(4);
            case 5 -> alternating(3, 4);
            case 6 -> fixed(3);
            case 7 -> alternating(2, 3);
            case 8 -> fixed(2);
            default -> throw new IllegalStateException("Unexpected effective points");
        };
    }

    public static Optional<CooldownSchedule> guardOrPenetration(int points) {
        return switch (ScalingSkillMath.effectivePoints(points)) {
            case 0 -> Optional.empty();
            case 1 -> fixed(7);
            case 2 -> fixed(6);
            case 3 -> alternating(5, 6);
            case 4 -> fixed(5);
            case 5 -> alternating(4, 5);
            case 6 -> fixed(4);
            case 7 -> alternating(3, 4);
            case 8 -> fixed(3);
            default -> throw new IllegalStateException("Unexpected effective points");
        };
    }

    private static Optional<CooldownSchedule> fixed(int cooldown) {
        return Optional.of(new CooldownSchedule(cooldown, cooldown));
    }

    private static Optional<CooldownSchedule> alternating(int firstCooldown, int secondCooldown) {
        return Optional.of(new CooldownSchedule(firstCooldown, secondCooldown));
    }

    public record CooldownSchedule(int firstCooldown, int secondCooldown) {
        public CooldownSchedule {
            if (firstCooldown < 0 || secondCooldown < firstCooldown) {
                throw new IllegalArgumentException("Invalid cooldown schedule");
            }
        }

        public Activation activate(Phase phase) {
            final var cooldown = phase.secondNext() ? secondCooldown : firstCooldown;
            return new Activation(cooldown, phase.next());
        }

        public boolean alternates() {
            return firstCooldown != secondCooldown;
        }
    }

    public record Phase(boolean secondNext) {
        public static final Phase INITIAL = new Phase(false);

        public Phase next() {
            return new Phase(!secondNext);
        }
    }

    public record Activation(int cooldown, Phase nextPhase) {
    }
}
