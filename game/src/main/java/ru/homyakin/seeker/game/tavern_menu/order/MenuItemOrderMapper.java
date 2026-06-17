package ru.homyakin.seeker.game.tavern_menu.order;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.tavern_menu.order.models.MenuItemOrder;
import ru.homyakin.seeker.game.tavern_menu.order.models.OrderStatus;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MenuItemOrderMapper {
    public static MenuItemOrder mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new MenuItemOrder(
            resultSet.getLong("id"),
            resultSet.getInt("menu_item_id"),
            PersonageId.from(resultSet.getLong("ordering_personage_id")),
            PersonageId.from(resultSet.getLong("accepting_personage_id")),
            resultSet.getTimestamp("expire_date_time").toLocalDateTime(),
            OrderStatus.findById(resultSet.getInt("status_id"))
        );
    }
}
