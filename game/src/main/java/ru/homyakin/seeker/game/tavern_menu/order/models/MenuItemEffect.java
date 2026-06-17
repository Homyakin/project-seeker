package ru.homyakin.seeker.game.tavern_menu.order.models;

import ru.homyakin.seeker.game.effect.Effect;

import java.time.LocalDateTime;

public record MenuItemEffect(
    Effect effect,
    LocalDateTime expireDateTime
) {
}
