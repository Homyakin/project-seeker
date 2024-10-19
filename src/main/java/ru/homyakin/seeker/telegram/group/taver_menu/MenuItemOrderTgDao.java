package ru.homyakin.seeker.telegram.group.taver_menu;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.tavern_menu.order.MenuItemOrderMapper;
import ru.homyakin.seeker.game.tavern_menu.order.models.MenuItemOrder;
import ru.homyakin.seeker.game.tavern_menu.order.models.OrderStatus;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

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
            .param("status_ids", List.of(OrderStatus.CREATED.id(), OrderStatus.CONSUMED.id()))
            .param("expire_date_time", expiringDateTime)
            .query(this::mapRow)
            .list();
    }

    public List<MenuItemOrder> findNotFinalForPersonageInGroup(PersonageId personageId, GroupTgId groupId) {
        final var sql = """
            SELECT mio.* FROM menu_item_order mio
            LEFT JOIN public.menu_item_order_tg miot ON mio.id = miot.menu_item_order_id
            WHERE accepting_personage_id = :personage_id
            AND status_id in (:status_ids)
            AND miot.grouptg_id = :grouptg_id
            """;
        return jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .param("status_ids", List.of(OrderStatus.CREATED.id(), OrderStatus.CONSUMED.id()))
            .param("grouptg_id", groupId.value())
            .query(MenuItemOrderMapper::mapRow)
            .list();
    }

    private MenuItemOrderTg mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new MenuItemOrderTg(
            rs.getLong("menu_item_order_id"),
            GroupTgId.from(rs.getLong("grouptg_id")),
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
        WHERE mio.status_id in (:status_ids) AND mio.expire_date_time <= :expire_date_time;
        """;
}

