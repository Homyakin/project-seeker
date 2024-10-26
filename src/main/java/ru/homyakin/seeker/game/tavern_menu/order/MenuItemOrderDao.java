package ru.homyakin.seeker.game.tavern_menu.order;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.tavern_menu.order.models.ExpiredOrder;
import ru.homyakin.seeker.game.tavern_menu.order.models.MenuItemOrder;
import ru.homyakin.seeker.game.tavern_menu.order.models.OrderStatus;
import ru.homyakin.seeker.game.tavern_menu.order.models.ThrowResult;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class MenuItemOrderDao {
    private final JdbcClient jdbcClient;

    public MenuItemOrderDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public long createOrder(
        int menuItemId,
        PersonageId orderingPersonageId,
        PersonageId acceptingPersonageId,
        GroupId groupId,
        LocalDateTime expireDateTime
    ) {
        final var sql = """
            INSERT INTO menu_item_order (menu_item_id, ordering_personage_id, accepting_personage_id,
                             pgroup_id, expire_date_time, status_id)
            VALUES (:menu_item_id, :ordering_personage_id, :accepting_personage_id, :pgroup_id, :expire_date_time, :status_id)
            RETURNING id
            """;

        return jdbcClient.sql(sql)
            .param("menu_item_id", menuItemId)
            .param("ordering_personage_id", orderingPersonageId.value())
            .param("accepting_personage_id", acceptingPersonageId.value())
            .param("pgroup_id", groupId.value())
            .param("expire_date_time", expireDateTime)
            .param("status_id", OrderStatus.CREATED.id())
            .query((rs, _) -> rs.getLong("id"))
            .single();
    }

    public Optional<MenuItemOrder> getById(long id) {
        return jdbcClient.sql(GET_BY_ID).param("id", id).query(MenuItemOrderMapper::mapRow).optional();
    }

    public void update(MenuItemOrder order) {
        final var sql = """
            UPDATE menu_item_order SET status_id = :status_id, expire_date_time = :expire_date_time
            WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("id", order.id())
            .param("status_id", order.status().id())
            .param("expire_date_time", order.expireDateTime())
            .update();
    }

    public void update(ExpiredOrder order) {
        final var sql = """
            UPDATE menu_item_order SET status_id = :status_id WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("id", order.id())
            .param("status_id", order.status().status.id())
            .update();
    }

    public void update(ThrowResult result) {
        Long targetPersonageId = null;
        boolean isTargetStaff = false;
        final var orderId = switch (result) {
            case ThrowResult.SelfThrow selfThrow -> {
                targetPersonageId = selfThrow.personage().id().value();
                yield selfThrow.orderId();
            }
            case ThrowResult.ThrowToNone throwToNone -> throwToNone.orderId();
            case ThrowResult.ThrowToOtherPersonage throwToOtherPersonage -> {
                targetPersonageId = throwToOtherPersonage.personage().id().value();
                yield throwToOtherPersonage.orderId();
            }
            case ThrowResult.ThrowToStaff throwToStaff -> {
                isTargetStaff = true;
                yield throwToStaff.orderId();
            }
        };

        final var sql = """
            UPDATE menu_item_order SET status_id = :status_id,
                throw_target_personage_id = :personage_id, is_throw_target_staff = :is_throw_target_staff
            WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("status_id", OrderStatus.THROWN.id())
            .param("id", orderId)
            .param("personage_id", targetPersonageId)
            .param("is_throw_target_staff", isTargetStaff)
            .update();
    }

    private static final String GET_BY_ID = "SELECT * FROM menu_item_order WHERE id = :id";
}

