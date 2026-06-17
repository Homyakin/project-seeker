package ru.homyakin.seeker.game.badge.entity;

import java.util.Map;

import ru.homyakin.seeker.common.models.BadgeId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

public record Badge(
    BadgeId id,
    BadgeView view,
    Map<Language, BadgeLocale> locales
) implements Localized<BadgeLocale> {
}
