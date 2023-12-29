package ru.homyakin.seeker.telegram.group.taver_menu;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.game.tavern_menu.models.OrderStatus;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.group.models.MenuItemOrderTg;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class MenuItemOrderTgDao {
    private final JdbcClient jdbcClient;

    public MenuItemOrderTgDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public MenuItemOrderTg insert(MenuItemOrderTg menuItemOrderTg) {
        jdbcClient.sql(INSERT)
            .param("menu_item_order_id", menuItemOrderTg.menuItemOrderId())
            .param("grouptg_id", menuItemOrderTg.groupTgId().value())
            .param("message_id", menuItemOrderTg.messageId())
            .update();

        return menuItemOrderTg;
    }

    public List<MenuItemOrderTg> findNotFinalWithLessExpireDateTime(LocalDateTime expiringDateTime) {
        return jdbcClient.sql(GET_WITH_LESS_EXPIRE_DATE_AND_STATUS)
            .param("status_id", OrderStatus.CREATED.id())
            .param("expire_date_time", expiringDateTime)
            .query(this::mapRow)
            .list();
    }

    private MenuItemOrderTg mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new MenuItemOrderTg(
            rs.getLong("menu_item_order_id"),
            GroupId.from(rs.getLong("grouptg_id")),
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

