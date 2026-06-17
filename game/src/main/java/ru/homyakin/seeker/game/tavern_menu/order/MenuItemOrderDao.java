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
import java.util.List;
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

    public List<MenuItemOrder> findNotFinalForPersonageInGroup(PersonageId personageId, GroupId groupId) {
        final var sql = """
            SELECT mio.* FROM menu_item_order mio
            WHERE accepting_personage_id = :personage_id
            AND status_id in (:status_ids)
            AND mio.pgroup_id = :pgroup_id
            """;
        return jdbcClient.sql(sql)
            .param("personage_id", personageId.value())
            .param("status_ids", List.of(OrderStatus.CREATED.id(), OrderStatus.CONSUMED.id()))
            .param("pgroup_id", groupId.value())
            .query(MenuItemOrderMapper::mapRow)
            .list();
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

    public void updateThrowTargets(long orderId, PersonageId targetPersonageId, GroupId targetGroupId) {
        final var sql = """
            UPDATE menu_item_order SET throw_target_personage_id = :personage_id,
             throw_target_pgroup_id = :throw_target_pgroup_id,
             status_id = :status_id
            WHERE id = :id
            """;
        jdbcClient.sql(sql)
            .param("id", orderId)
            .param("personage_id", targetPersonageId.value())
            .param("throw_target_pgroup_id", targetGroupId.value())
            .param("status_id", OrderStatus.THROWN.id())
            .update();
    }

    public Optional<LocalDateTime> lastThrowFromGroup(GroupId groupId) {
        final var sql = """
            SELECT expire_date_time FROM menu_item_order mio
            WHERE mio.pgroup_id = :pgroup_id
            AND throw_target_pgroup_id is not null
            ORDER BY expire_date_time DESC
            LIMIT 1
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .query((rs, _) -> rs.getTimestamp("expire_date_time").toLocalDateTime())
            .optional();
    }

    public Optional<LocalDateTime> lastThrowToGroup(GroupId groupId) {
        final var sql = """
            SELECT expire_date_time FROM menu_item_order mio
            WHERE throw_target_pgroup_id = :pgroup_id
            ORDER BY expire_date_time DESC
            LIMIT 1
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .query((rs, _) -> rs.getTimestamp("expire_date_time").toLocalDateTime())
            .optional();
    }

    private static final String GET_BY_ID = "SELECT * FROM menu_item_order WHERE id = :id";
}

