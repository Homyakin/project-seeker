package ru.homyakin.seeker.game.tavern_menu.order.models;

import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.tavern_menu.menu.models.Category;

public sealed interface ThrowResult {

    record ThrowToNone(long orderId, Money cost, Category category) implements ThrowResult {
    }

    record ThrowToOtherPersonage(long orderId, Money cost, Personage personage, Effect effect, Category category) implements ThrowResult {
    }

    record SelfThrow(long orderId, Money cost, Personage personage, Effect effect, Category category) implements ThrowResult {
    }

    record ThrowToStaff(long orderId, Money cost, Effect effect, Category category) implements ThrowResult {
    }
}
