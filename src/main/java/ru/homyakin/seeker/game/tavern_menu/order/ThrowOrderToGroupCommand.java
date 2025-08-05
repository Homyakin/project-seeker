package ru.homyakin.seeker.game.tavern_menu.order;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.game.group.action.personage.RandomGroupPersonage;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.tavern_menu.order.models.ThrowToGroupError;
import ru.homyakin.seeker.game.tavern_menu.order.models.ThrowToGroupResult;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class ThrowOrderToGroupCommand {
    private final OrderService orderService;
    private final GetGroup getGroup;
    private final PersonageService personageService;
    private final RandomGroupPersonage randomGroupPersonage;
    private final MenuItemOrderDao menuItemOrderDao;
    private final OrderConfig config;

    public ThrowOrderToGroupCommand(
        OrderService orderService,
        GetGroup getGroup,
        PersonageService personageService,
        RandomGroupPersonage randomGroupPersonage,
        MenuItemOrderDao menuItemOrderDao,
        OrderConfig config
    ) {
        this.orderService = orderService;
        this.getGroup = getGroup;
        this.personageService = personageService;
        this.randomGroupPersonage = randomGroupPersonage;
        this.menuItemOrderDao = menuItemOrderDao;
        this.config = config;
    }

    public Either<ThrowToGroupError, ThrowToGroupResult> execute(
        PersonageId personageId,
        GroupId fromGroupId,
        String targetTag
    ) {
        final var group = getGroup.forceGet(fromGroupId);
        if (!group.isRegistered()) {
            return Either.left(ThrowToGroupError.NotRegisteredGroup.INSTANCE);
        }
        if (group.isSameTag(targetTag)) {
            return Either.left(ThrowToGroupError.ThrowToThisGroup.INSTANCE);
        }

        final var personage = personageService.getByIdForce(personageId);
        if (!personage.isGroupMember(group)) {
            return Either.left(ThrowToGroupError.NotGroupMember.INSTANCE);
        }

        final var lastThrowDate = menuItemOrderDao.lastThrowFromGroup(group.id());
        if (lastThrowDate.isPresent() && !TimeUtils.isTimePassed(lastThrowDate.get(), config.throwGroupTimeout()) && targetTag != 'SLOT') {
            final var remaining = TimeUtils.remainingTime(lastThrowDate.get().plus(config.throwGroupTimeout()));
            return Either.left(new ThrowToGroupError.ThrowingGroupTimeout(remaining));
        }

        final var targetGroup = getGroup.getByTag(targetTag);
        if (targetGroup.isEmpty()) {
            return Either.left(ThrowToGroupError.TargetGroupNotFound.INSTANCE);
        }

        final var lastThrowTargetDate = menuItemOrderDao.lastThrowToGroup(targetGroup.get().id());
        if (lastThrowTargetDate.isPresent()
            && !TimeUtils.isTimePassed(lastThrowTargetDate.get(), config.throwTargetGroupTimeout()) && targetTag != 'SLOT') {
            return Either.left(ThrowToGroupError.TargetGroupTimeout.INSTANCE);
        }

        final var targetPersonageResult = randomGroupPersonage.randomMember(targetGroup.get().id());
        if (targetPersonageResult.isLeft()) {
            return Either.left(ThrowToGroupError.InternalError.INSTANCE);
        }
        if (targetPersonageResult.get().isEmpty()) {
            return Either.left(ThrowToGroupError.TargetGroupIsEmpty.INSTANCE);
        }
        final var targetPersonage = targetPersonageResult.get()
            .map(personageService::getByIdForce)
            .orElseThrow();

        return orderService.throwOrder(
            personage,
            group.id(),
            targetGroup.get().id(),
            targetPersonage
        ).map(
            it -> new ThrowToGroupResult(
                it.cost(),
                personage,
                targetPersonage,
                group,
                targetGroup.get(),
                it.effect(),
                it.category()
            )
        ).mapLeft(it -> it);
    }
}
