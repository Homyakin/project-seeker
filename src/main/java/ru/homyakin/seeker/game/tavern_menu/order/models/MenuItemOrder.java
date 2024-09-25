package ru.homyakin.seeker.game.tavern_menu.order.models;

import java.time.Duration;
import java.time.LocalDateTime;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public record MenuItemOrder(
    long id,
    int menuItemId,
    PersonageId orderingPersonageId,
    PersonageId acceptingPersonageId,
    LocalDateTime expireDateTime,
    OrderStatus status
) {
    public Either<ConsumeOrderError, MenuItemOrder> consume(
        Personage consumer,
        LocalDateTime now,
        Duration timeToThrow
    ) {
        if (!acceptingPersonageId().equals(consumer.id())) {
            return Either.left(ConsumeOrderError.WrongConsumer.INSTANCE);
        }
        if (status != OrderStatus.CREATED) {
            logger.error("Final status in consuming order: " + id);
            return Either.left(ConsumeOrderError.AlreadyFinalStatus.INSTANCE);
        }

        return Either.right(
            new MenuItemOrder(
                id,
                menuItemId,
                orderingPersonageId,
                acceptingPersonageId,
                now.plus(timeToThrow),
                OrderStatus.CONSUMED
            )
        );
    }

    public Either<ExpireOrderError, ExpiredOrder> expire() {
        return switch (status) {
            case CREATED -> Either.right(new ExpiredOrder(id, ExpiredOrder.Status.EXPIRED));
            case CONSUMED -> Either.right(new ExpiredOrder(id, ExpiredOrder.Status.CONSUMED_AND_EXPIRED));
            default -> {
                logger.error("Incorrect order status to be expired: {}", status);
                yield Either.left(ExpireOrderError.InvalidStatus.INSTANCE);
            }
        };
    }

    public MenuItemOrder techCancel() {
        return new MenuItemOrder(
            id,
            menuItemId,
            orderingPersonageId,
            acceptingPersonageId,
            expireDateTime,
            OrderStatus.TECH_CANCEL
        );
    }

    public boolean canThrow() {
        return status == OrderStatus.CONSUMED;
    }

    private static final Logger logger = LoggerFactory.getLogger(MenuItemOrder.class);
}
