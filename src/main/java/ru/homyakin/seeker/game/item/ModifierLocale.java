package ru.homyakin.seeker.game.item;

import java.util.Map;
import ru.homyakin.seeker.locale.WordForm;

public record ModifierLocale(
    Map<WordForm, String> form
) {
}
