package ru.homyakin.seeker.locale.duel;

import java.util.Optional;

public record DuelText(
    String text,
    Optional<Integer> initiatorPosition,
    Optional<Integer> acceptorPosition
) {
}
