package ru.homyakin.seeker.game.spin.entity;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.time.LocalDate;
import java.util.Optional;

public interface EverydaySpinStorage {
    Optional<PersonageId> findPersonageIdByGroupIdAndDate(GroupId groupId, LocalDate date);

    void save(GroupId groupId, PersonageId personageId, LocalDate date);
}
