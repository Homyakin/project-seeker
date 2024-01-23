package ru.homyakin.seeker.game.tavern_menu;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.tavern_menu.models.Category;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItem;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemLocale;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingMenuItem;
import ru.homyakin.seeker.locale.Language;

@Component
public class MenuDao {
    private static final String GET_AVAILABLE_MENU = "SELECT * FROM menu_item WHERE is_available = true";
    private static final String GET_MENU_ITEM_BY_CODE = "SELECT * FROM menu_item WHERE code = :code";
    private static final String GET_MENU_ITEM_BY_ID = "SELECT * FROM menu_item WHERE id = :id";

    private static final String GET_MENU_ITEM_LOCALES = "SELECT * FROM menu_item_locale WHERE menu_item_id = :menu_item_id";
    private static final String SAVE_ITEM = """
        INSERT INTO menu_item (price, is_available, category_id, code)
        VALUES (:price, :is_available, :category_id, :code)
        ON CONFLICT (code)
        DO UPDATE SET price = :price, is_available = :is_available, category_id = :category_id
        RETURNING id
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
        return jdbcClient.sql(GET_MENU_ITEM_BY_ID)
            .param("id", id)
            .query(this::mapMenuItem)
            .optional()
            .map(it -> it.toMenuItem(getMenuItemLocales(it.id)));
    }

    public Optional<MenuItem> getMenuItem(String code) {
        return jdbcClient.sql(GET_MENU_ITEM_BY_CODE)
            .param("code", code)
            .query(this::mapMenuItem)
            .optional()
            .map(it -> it.toMenuItem(getMenuItemLocales(it.id)));
    }

    @Transactional
    public void saveItem(SavingMenuItem menuItem) {
        final int id = jdbcClient.sql(SAVE_ITEM)
            .param("price", menuItem.price().value())
            .param("is_available", menuItem.isAvailable())
            .param("category_id", menuItem.category().id())
            .param("code", menuItem.code())
            .query((rs, rowNum) -> rs.getInt("id"))
            .single();
        menuItem.locales().forEach(
            locale -> saveLocale(id, locale)
        );
    }

    private void saveLocale(int menuItemId, MenuItemLocale locale) {
        jdbcClient.sql(SAVE_LOCALE)
            .param("menu_item_id", menuItemId)
            .param("language_id", locale.language().id())
            .param("name", locale.name())
            .param("consume_template", locale.consumeTemplate())
            .update();
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
            rs.getString("code"),
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
        String code,
        int price,
        boolean isAvailable,
        Category category
    ) {
        public MenuItem toMenuItem(List<MenuItemLocale> locales) {
            return new MenuItem(
                id,
                code,
                new Money(price),
                isAvailable,
                category,
                locales
            );
        }
    }
}

