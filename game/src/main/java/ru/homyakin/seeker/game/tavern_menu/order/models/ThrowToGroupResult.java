package ru.homyakin.seeker.game.tavern_menu.order.models;

import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.tavern_menu.menu.models.Category;

public record ThrowToGroupResult(
    Money cost,
    Personage throwing,
    Personage target,
    Group throwingGroup,
    Group targetGroup,
    Effect effect,
    Category category
) {
}
