package ru.homyakin.seeker.game.tavern_menu;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.List;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.tavern_menu.models.Category;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItem;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemLocale;
import ru.homyakin.seeker.locale.Language;

@Component
public class MenuDao {
    private static final String GET_AVAILABLE_MENU = "SELECT * FROM menu_item WHERE is_available = true";
    private static final String GET_AVAILABLE_MENU_ITEM = "SELECT * FROM menu_item WHERE id = :id and is_available = true";

    private static final String GET_MENU_ITEM_LOCALES = "SELECT * FROM menu_item_locale WHERE menu_item_id = :menu_item_id";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MenuDao(DataSource dataSource) {
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public List<MenuItem> getAvailableMenu() {
        return jdbcTemplate.query(GET_AVAILABLE_MENU, this::mapMenuItem)
            .stream()
            .map(it -> it.toMenuItem(getMenuItemLocales(it.id)))
            .toList();
    }

    public Optional<MenuItem> getAvailableMenuItem(int id) {
        final var param = Collections.singletonMap("id", id);
        return jdbcTemplate.query(GET_AVAILABLE_MENU_ITEM, param, this::mapMenuItem)
            .stream()
            .findFirst()
            .map(it -> it.toMenuItem(getMenuItemLocales(id)));
    }

    private List<MenuItemLocale> getMenuItemLocales(int menuItemId) {
        final var params = Collections.singletonMap("menu_item_id", menuItemId);
        return jdbcTemplate.query(
            GET_MENU_ITEM_LOCALES,
            params,
            this::mapLocale
        );
    }

    private MenuItemWithoutLocale mapMenuItem(ResultSet rs, int rowNum) throws SQLException {
        return new MenuItemWithoutLocale(
            rs.getInt("id"),
            rs.getInt("price"),
            rs.getBoolean("is_available"),
            Category.getById(rs.getInt("category_id"))
        );
    }

    private MenuItemLocale mapLocale(ResultSet rs, int rowNum) throws SQLException {
        return new MenuItemLocale(
            Language.getOrDefault(rs.getInt("language_id")),
            rs.getString("name"),
            (String[]) rs.getArray("order_template").getArray()
        );
    }

    private record MenuItemWithoutLocale(
        int id,
        int price,
        boolean isAvailable,
        Category category
    ) {
        public MenuItem toMenuItem(List<MenuItemLocale> locales) {
            return new MenuItem(
                id,
                new Money(price),
                isAvailable,
                category,
                locales
            );
        }
    }
}

