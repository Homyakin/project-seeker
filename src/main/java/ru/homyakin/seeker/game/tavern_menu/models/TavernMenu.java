package ru.homyakin.seeker.game.tavern_menu.models;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.tavern_menu.TavernMenuLocalization;

public record TavernMenu(
    Map<Category, List<MenuItem>> categoryToItems
) {
    private static final List<Category> CATEGORY_ORDER = List.of(Category.DRINK, Category.MAIN_DISH);

    public String tavernMenuText(Language language) {
        final var text = new StringBuilder(TavernMenuLocalization.menuHeader(language));
        for (final var category: CATEGORY_ORDER) {
            if (!categoryToItems.containsKey(category)) {
                continue;
            }
            text.append("\n\n").append(category.getText(language)).append(":");
            final var itemsSortedByPrice = categoryToItems.get(category).stream().sorted(Comparator.comparing(MenuItem::price)).toList();
            for (final var menuItem: itemsSortedByPrice) {
                text.append("\n").append(menuItem.menuPositionText(language));
            }
        }
        return text.toString();
    }
}
