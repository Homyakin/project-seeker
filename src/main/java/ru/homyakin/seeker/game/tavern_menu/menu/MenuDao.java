package ru.homyakin.seeker.game.tavern_menu.menu;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.tavern_menu.menu.models.Category;
import ru.homyakin.seeker.game.tavern_menu.menu.models.MenuItem;
import ru.homyakin.seeker.game.tavern_menu.menu.models.MenuItemLocale;
import ru.homyakin.seeker.game.tavern_menu.menu.models.MenuItemRarity;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingMenuItem;
import ru.homyakin.seeker.locale.Language;

@Component
public class MenuDao {
    private static final String GET_AVAILABLE_MENU = "SELECT * FROM menu_item WHERE is_available = true";
    private static final String GET_MENU_ITEM_BY_CODE = "SELECT * FROM menu_item WHERE code = :code";
    private static final String GET_MENU_ITEM_BY_ID = "SELECT * FROM menu_item WHERE id = :id";

    private static final String GET_MENU_ITEM_LOCALES = "SELECT * FROM menu_item_locale WHERE menu_item_id = :menu_item_id";
    private static final String SAVE_ITEM = """
        INSERT INTO menu_item (is_available, category_id, code, rarity_id, effect_characteristic)
        VALUES (:is_available, :category_id, :code, :rarity_id, :effect_characteristic)
        ON CONFLICT (code)
        DO UPDATE SET rarity_id = :rarity_id, is_available = :is_available, category_id = :category_id,
              effect_characteristic = :effect_characteristic
        RETURNING id
        """;
    private static final String SAVE_LOCALE = """
        INSERT INTO menu_item_locale (menu_item_id, language_id, name, consume_template) 
        VALUES (:menu_item_id, :language_id, :name, :consume_template)
        ON CONFLICT (menu_item_id, language_id)
        DO UPDATE SET name = :name, consume_template = :consume_template
        """;

    private final JdbcClient jdbcClient;
    private final MenuConfig config;

    public MenuDao(DataSource dataSource, MenuConfig config) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.config = config;
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
            .param("rarity_id", menuItem.rarity().id())
            .param("is_available", menuItem.isAvailable())
            .param("category_id", menuItem.category().id())
            .param("code", menuItem.code())
            .param("effect_characteristic", menuItem.effectCharacteristic().name())
            .query((rs, _) -> rs.getInt("id"))
            .single();
        menuItem.locales().forEach(
            (language, locale) -> saveLocale(id, language, locale)
        );
    }

    private void saveLocale(int menuItemId, Language language, MenuItemLocale locale) {
        jdbcClient.sql(SAVE_LOCALE)
            .param("menu_item_id", menuItemId)
            .param("language_id", language.id())
            .param("name", locale.name())
            .param("consume_template", locale.consumeTemplate())
            .update();
    }

    @SuppressWarnings("unchecked")
    private Map<Language, MenuItemLocale> getMenuItemLocales(int menuItemId) {
        final var list = jdbcClient.sql(GET_MENU_ITEM_LOCALES)
            .param("menu_item_id", menuItemId)
            .query(this::mapLocale)
            .list();
        return Map.ofEntries(list.toArray(new Map.Entry[0]));
    }

    private MenuItemWithoutLocale mapMenuItem(ResultSet rs, int rowNum) throws SQLException {
        final var effectCharacteristic = EffectCharacteristic.valueOf(rs.getString("effect_characteristic"));
        final var rarity = MenuItemRarity.findById(rs.getInt("rarity_id"));
        final Effect effect = switch (effectCharacteristic) {
            case ATTACK, HEALTH -> new Effect.Multiplier(config.multiplyPercentByRarity(rarity), effectCharacteristic);
            case STRENGTH, WISDOM, AGILITY -> new Effect.Add(config.addValueByRarity(rarity), effectCharacteristic);
        };
        return new MenuItemWithoutLocale(
            rs.getInt("id"),
            rs.getString("code"),
            config.priceByRarity(rarity),
            rs.getBoolean("is_available"),
            Category.getById(rs.getInt("category_id")),
            effect
        );
    }

    private Map.Entry<Language, MenuItemLocale> mapLocale(ResultSet rs, int rowNum) throws SQLException {
        return Map.entry(
            Language.getOrDefault(rs.getInt("language_id")),
            new MenuItemLocale(
                rs.getString("name"),
                (String[]) rs.getArray("consume_template").getArray()
            )
        );
    }

    private record MenuItemWithoutLocale(
        int id,
        String code,
        Money price,
        boolean isAvailable,
        Category category,
        Effect effect
    ) {
        public MenuItem toMenuItem(Map<Language, MenuItemLocale> locales) {
            return new MenuItem(
                id,
                code,
                price,
                isAvailable,
                category,
                locales,
                effect
            );
        }
    }
}

