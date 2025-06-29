package ru.homyakin.seeker.game.badge.entity;

import ru.homyakin.seeker.infrastructure.init.saving_models.SavingBadge;

import java.util.Optional;

public interface BadgeStorage {
    void save(SavingBadge badge);

    Optional<Badge> getByCode(String code);
}
