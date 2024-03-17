package ru.homyakin.seeker.infrastructure.init.saving_models;

import java.util.List;
import ru.homyakin.seeker.game.item.models.Modifier;

public record ItemModifiers(
    List<Modifier> modifier
) {
}
