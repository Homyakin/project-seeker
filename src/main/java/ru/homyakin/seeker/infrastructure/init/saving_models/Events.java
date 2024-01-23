package ru.homyakin.seeker.infrastructure.init.saving_models;

import java.util.List;

public record Events(
    List<SavingEvent> event
) {
}
