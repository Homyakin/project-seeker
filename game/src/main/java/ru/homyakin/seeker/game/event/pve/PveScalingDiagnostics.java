package ru.homyakin.seeker.game.event.pve;

import java.util.Map;
import java.util.Objects;

/** Ephemeral technical generation metadata, separate from displayed character power. */
public record PveScalingDiagnostics(
    String assessmentVersion,
    double participantAssessment,
    Map<String, Double> parameters
) {
    public PveScalingDiagnostics {
        Objects.requireNonNull(assessmentVersion, "assessmentVersion");
        if (assessmentVersion.isBlank()) {
            throw new IllegalArgumentException("Assessment version must not be blank");
        }
        requireFiniteNonNegative(participantAssessment);
        parameters = Map.copyOf(parameters);
        parameters.values().forEach(PveScalingDiagnostics::requireFiniteNonNegative);
    }

    private static void requireFiniteNonNegative(double value) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException("Scaling diagnostic values must be finite and nonnegative: " + value);
        }
    }
}
