package ru.homyakin.seeker.game.tavern_menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.tavern_menu.models.Category;
import ru.homyakin.seeker.game.tavern_menu.models.TavernMenu;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItem;

@Component
public class MenuService {
    private final MenuDao menuDao;

    public MenuService(MenuDao menuDao) {
        this.menuDao = menuDao;
    }

    public TavernMenu getAvailableMenu() {
        //TODO кэш
        final var map = new HashMap<Category, List<MenuItem>>();
        final var items = menuDao.getAvailableMenu();
        for (final var item: items) {
            if (map.containsKey(item.category())) {
                map.get(item.category()).add(item);
            } else {
                map.put(item.category(), new ArrayList<>(List.of(item)));
            }
        }
        return new TavernMenu(map);
    }

    public Optional<MenuItem> getAvailableMenuItem(int id) {
        return menuDao.getAvailableMenuItem(id);
    }
}

