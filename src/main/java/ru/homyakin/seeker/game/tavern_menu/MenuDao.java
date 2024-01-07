package ru.homyakin.seeker.game.tavern_menu;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.tavern_menu.models.Category;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItem;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemLocale;
import ru.homyakin.seeker.locale.Language;

@Component
public class MenuDao {
    private static final String GET_AVAILABLE_MENU = "SELECT * FROM menu_item WHERE is_available = true";
    private static final String GET_MENU_ITEM = "SELECT * FROM menu_item WHERE id = :id";

    private static final String GET_MENU_ITEM_LOCALES = "SELECT * FROM menu_item_locale WHERE menu_item_id = :menu_item_id";
    private static final String SAVE_ITEM = """
        INSERT INTO menu_item (id, price, is_available, category_id) 
        VALUES (:id, :price, :is_available, :category_id)
        ON CONFLICT (id)
        DO UPDATE SET price = :price, is_available = :is_available, category_id = :category_id
        """;
    private static final String SAVE_LOCALE = """
        INSERT INTO menu_item_locale (menu_item_id, language_id, name, consume_template) 
        VALUES (:menu_item_id, :language_id, :name, :consume_template)
        ON CONFLICT (menu_item_id, language_id)
        DO UPDATE SET name = :name, consume_template = :consume_template
        """;

    private final JdbcClient jdbcClient;

    public MenuDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    public List<MenuItem> getAvailableMenu() {
        return jdbcClient.sql(GET_AVAILABLE_MENU)
            .query(this::mapMenuItem)
            .list()
            .stream()
            .map(it -> it.toMenuItem(getMenuItemLocales(it.id)))
            .toList();
    }

    public Optional<MenuItem> getMenuItem(int id) {
        return jdbcClient.sql(GET_MENU_ITEM)
            .param("id", id)
            .query(this::mapMenuItem)
            .optional()
            .map(it -> it.toMenuItem(getMenuItemLocales(id)));
    }

    @Transactional
    public void saveItem(MenuItem menuItem) {
        jdbcClient.sql(SAVE_ITEM)
            .param("id", menuItem.id())
            .param("price", menuItem.price().value())
            .param("is_available", menuItem.isAvailable())
            .param("category_id", menuItem.category().id())
            .update();
        saveLocales(menuItem);
    }

    private void saveLocales(MenuItem menuItem) {
        menuItem.locales().forEach(
            locale -> jdbcClient.sql(SAVE_LOCALE)
                .param("menu_item_id", menuItem.id())
                .param("language_id", locale.language().id())
                .param("name", locale.name())
                .param("consume_template", locale.consumeTemplate())
                .update()
        );
    }

    private List<MenuItemLocale> getMenuItemLocales(int menuItemId) {
        return jdbcClient.sql(GET_MENU_ITEM_LOCALES)
            .param("menu_item_id", menuItemId)
            .query(this::mapLocale)
            .list();
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
            (String[]) rs.getArray("consume_template").getArray()
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

