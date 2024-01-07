package ru.homyakin.seeker.infrastructure.init;

import java.util.List;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItem;

public record Items(
    List<MenuItem> item
) {
}
