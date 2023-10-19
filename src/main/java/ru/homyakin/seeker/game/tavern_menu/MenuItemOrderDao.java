package ru.homyakin.seeker.game.tavern_menu;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemOrder;
import ru.homyakin.seeker.game.tavern_menu.models.OrderStatus;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class MenuItemOrderDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsert;

    public MenuItemOrderDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        simpleJdbcInsert = new SimpleJdbcInsert(dataSource)
            .withTableName("menu_item_order")
            .usingColumns(
                "menu_item_id",
                "ordering_personage_id",
                "accepting_personage_id",
                "expire_date_time",
                "status_id"
            )
            .usingGeneratedKeyColumns("id");
    }

    public long createOrder(
        int menuItemId,
        PersonageId orderingPersonageId,
        PersonageId acceptingPersonageId,
        LocalDateTime expireDateTime
    ) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("menu_item_id", menuItemId)
            .addValue("ordering_personage_id", orderingPersonageId.value())
            .addValue("accepting_personage_id", acceptingPersonageId.value())
            .addValue("expire_date_time", expireDateTime)
            .addValue("status_id", OrderStatus.CREATED.id());

        return simpleJdbcInsert.executeAndReturnKey(parameters).longValue();
    }

    public Optional<MenuItemOrder> getById(long id) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
            .addValue("id", id);

        return jdbcTemplate.query(GET_BY_ID, parameters, this::mapRow).stream().findFirst();
    }

    public void updateStatus(long orderId, OrderStatus status) {
        final var paramMap = new MapSqlParameterSource()
            .addValue("status_id", status.id())
            .addValue("orderId", orderId);

        jdbcTemplate.update(UPDATE_STATUS, paramMap);
    }

    private MenuItemOrder mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return new MenuItemOrder(
            resultSet.getLong("id"),
            resultSet.getInt("menu_item_id"),
            PersonageId.from(resultSet.getLong("ordering_personage_id")),
            PersonageId.from(resultSet.getLong("accepting_personage_id")),
            resultSet.getTimestamp("expire_date_time").toLocalDateTime(),
            OrderStatus.findById(resultSet.getInt("status_id"))
        );
    }

    private static final String UPDATE_STATUS = "UPDATE menu_item_order SET status_id = :status_id WHERE id = :orderId";
    private static final String GET_BY_ID = "SELECT * FROM menu_item_order WHERE id = :id";
}

