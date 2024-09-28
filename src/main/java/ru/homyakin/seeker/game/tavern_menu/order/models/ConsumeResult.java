package ru.homyakin.seeker.game.tavern_menu.order.models;

import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.tavern_menu.menu.models.MenuItem;

public record ConsumeResult(
    MenuItem item,
    PersonageEffect effect,
    Personage personage
) {
}
