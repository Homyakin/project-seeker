package ru.homyakin.seeker.infrastructure.init;

import java.util.List;

public record Events(
    List<SavingEvent> event
) {
}
