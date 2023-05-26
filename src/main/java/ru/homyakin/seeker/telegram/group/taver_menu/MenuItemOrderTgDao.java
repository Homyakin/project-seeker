package ru.homyakin.seeker.telegram.group.taver_menu;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.game.tavern_menu.models.OrderStatus;
import ru.homyakin.seeker.telegram.group.models.MenuItemOrderTg;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class MenuItemOrderTgDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MenuItemOrderTgDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public MenuItemOrderTg insert(MenuItemOrderTg menuItemOrderTg) {
        final var params = new MapSqlParameterSource()
            .addValue("menu_item_order_id", menuItemOrderTg.menuItemOrderId())
            .addValue("grouptg_id", menuItemOrderTg.groupTgId())
            .addValue("message_id", menuItemOrderTg.messageId());

        jdbcTemplate.update(INSERT, params);

        return menuItemOrderTg;
    }

    public List<MenuItemOrderTg> findNotFinalWithLessExpireDateTime(LocalDateTime expiringDateTime) {
        final var params = new MapSqlParameterSource()
            .addValue("status_id", OrderStatus.CREATED.id())
            .addValue("expire_date_time", expiringDateTime);

        return jdbcTemplate.query(GET_WITH_LESS_EXPIRE_DATE_AND_STATUS, params, this::mapRow);
    }

    private MenuItemOrderTg mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new MenuItemOrderTg(
            rs.getLong("menu_item_order_id"),
            rs.getLong("grouptg_id"),
            rs.getInt("message_id")
        );
    }

    private static final String INSERT = """
        INSERT INTO menu_item_order_tg (menu_item_order_id, grouptg_id, message_id)
        VALUES (:menu_item_order_id, :grouptg_id, :message_id)
        """;
    private static final String GET_WITH_LESS_EXPIRE_DATE_AND_STATUS = """
        SELECT miot.* FROM menu_item_order_tg miot
        LEFT JOIN menu_item_order mio on mio.id = miot.menu_item_order_id
        WHERE mio.status_id = :status_id AND mio.expire_date_time <= :expire_date_time;
        """;
}

