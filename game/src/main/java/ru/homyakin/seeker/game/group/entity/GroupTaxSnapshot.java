package ru.homyakin.seeker.game.group.entity;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Group tax state for messaging after DB has been synced from the lazy formula when applicable.
 */
public record GroupTaxSnapshot(
    int taxLevel,
    Optional<LocalDateTime> nextRecalcAt,
    int taxAfterNextRecalc,
    int memberCount,
    int leavedCount
) {
}
