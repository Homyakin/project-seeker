package ru.homyakin.seeker.game.group.action;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupConfig;
import ru.homyakin.seeker.game.group.entity.GroupTaxSnapshot;
import ru.homyakin.seeker.game.group.entity.GroupTaxStorage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class GroupTaxService {
    private static final Logger logger = LoggerFactory.getLogger(GroupTaxService.class);

    private final GroupTaxStorage taxStorage;
    private final GroupConfig groupConfig;

    public GroupTaxService(GroupTaxStorage taxStorage, GroupConfig groupConfig) {
        this.taxStorage = taxStorage;
        this.groupConfig = groupConfig;
    }

    public int currentTax(GroupId groupId) {
        final var row = taxStorage.loadTaxRow(groupId);
        return GroupTaxFormulas.computeNextTax(
            row.effectiveTax(),
            row.memberCount(),
            row.lastTaxUpdate(),
            TimeUtils.moscowTime(),
            groupConfig.groupTaxRecalcInterval()
        );
    }

    @Transactional
    public void recalculateTax(GroupId groupId) {
        final var row = taxStorage.lockTax(groupId);
        final var now = TimeUtils.moscowTime();
        final int nextTax = GroupTaxFormulas.computeNextTax(
            row.effectiveTax(),
            row.memberCount(),
            row.lastTaxUpdate(),
            now,
            groupConfig.groupTaxRecalcInterval()
        );
        if (nextTax >= row.effectiveTax()) {
            return;
        }
        logger.info("Recalculated tax for group {}. Old={}, new={}", groupId, row.effectiveTax(), nextTax);
        final int delta = row.effectiveTax() - nextTax;
        final int leaved = taxStorage.countLeaved(groupId);
        final int deleteCount = Math.min(delta, leaved);
        if (deleteCount > 0) {
            taxStorage.deleteOldestLeaved(groupId, deleteCount);
        }
        final int newEffective = row.effectiveTax() - deleteCount;
        if (GroupTaxFormulas.computeNextTax(newEffective, row.memberCount()) == newEffective) {
            taxStorage.updateTaxRow(groupId, newEffective, Optional.empty());
        } else {
            taxStorage.updateTaxRow(groupId, newEffective, Optional.of(now));
        }
    }

    public GroupTaxSnapshot groupTaxSnapshot(GroupId groupId) {
        final var row = taxStorage.loadTaxRow(groupId);
        final int taxAfterNext = GroupTaxFormulas.computeNextTax(row.effectiveTax(), row.memberCount());
        final var interval = groupConfig.groupTaxRecalcInterval();
        final var nextAt = taxAfterNext != row.effectiveTax()
            ? row.lastTaxUpdate().map(t -> t.plus(interval))
            : Optional.<LocalDateTime>empty();
        return new GroupTaxSnapshot(
            row.effectiveTax(),
            nextAt,
            taxAfterNext,
            row.memberCount(),
            taxStorage.countLeaved(groupId)
        );
    }

    @Transactional
    public void applyJoin(GroupId groupId, PersonageId personageId) {
        final var row = taxStorage.lockTax(groupId);
        final boolean returning = taxStorage.deleteLeavedIfExists(groupId, personageId);
        final int newEffective = returning ? row.effectiveTax() : row.effectiveTax() + 1;
        taxStorage.updateTaxRow(groupId, newEffective, row.lastTaxUpdate());
    }

    @Transactional
    public void applyLeave(GroupId groupId, PersonageId personageId) {
        final var row = taxStorage.lockTax(groupId);
        final var now = TimeUtils.moscowTime();
        taxStorage.insertLeaved(groupId, personageId, now);
        taxStorage.updateTaxRow(groupId, row.effectiveTax(), Optional.of(now));
    }
}
