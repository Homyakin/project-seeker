package ru.homyakin.seeker.game.event.pve;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PveScalingDiagnosticsTest {
    @Test
    void keepsImmutableDiagnosticParametersSeparateFromCalibrationAndEnemies() {
        final var parameters = new HashMap<>(Map.of("frontWeight", 1.5));
        final var diagnostics = new PveScalingDiagnostics(PveTeamAssessment.VERSION, 10, parameters);
        parameters.put("frontWeight", 999.0);

        Assertions.assertEquals(Map.of("frontWeight", 1.5), diagnostics.parameters());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> diagnostics.parameters().clear());
        final var calibration = new PveCharacteristicScaling.Result(1, 10, 10,
            PveCharacteristicScaling.Status.MATCHED, 1);
        final var oldConstructor = new GeneratedPveEnemies(new ArrayList<>(), calibration);
        Assertions.assertTrue(oldConstructor.diagnostics().isEmpty());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> oldConstructor.enemies().clear());
    }

    @Test
    void rejectsMissingVersionsAndNonFiniteOrNegativeDiagnosticValues() {
        Assertions.assertThrows(NullPointerException.class, () -> new PveScalingDiagnostics(null, 0, Map.of()));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new PveScalingDiagnostics(" ", 0, Map.of()));
        Assertions.assertThrows(NullPointerException.class, () -> new PveScalingDiagnostics("V1", 0, null));
        for (final var value : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> new PveScalingDiagnostics("V1", value, Map.of()));
            Assertions.assertThrows(IllegalArgumentException.class, () ->
                new PveScalingDiagnostics("V1", 0, Map.of("parameter", value)));
        }
    }
}
