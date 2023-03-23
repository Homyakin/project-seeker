package ru.homyakin.seeker.locale.duel;

import java.util.Optional;

public record EndDuelText(
    String text,
    Optional<Integer> winnerPosition,
    Optional<Integer> looserPosition
) {
}
