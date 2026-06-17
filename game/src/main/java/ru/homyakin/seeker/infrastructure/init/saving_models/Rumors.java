package ru.homyakin.seeker.infrastructure.init.saving_models;

import java.util.List;
import ru.homyakin.seeker.game.rumor.Rumor;

public record Rumors(
    List<Rumor> rumor
) {
}
