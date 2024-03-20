package ru.homyakin.seeker.infrastructure.init.saving_models.item;

import java.util.List;

public record ItemModifiers(
    List<SavingModifier> modifier
) {
}
