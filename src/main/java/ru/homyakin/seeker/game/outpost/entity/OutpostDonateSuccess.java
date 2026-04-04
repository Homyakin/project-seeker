package ru.homyakin.seeker.game.outpost.entity;

import java.util.List;

public record OutpostDonateSuccess(
    int materialsFromItem,
    int deliveredAfter,
    int materialsRequired,
    boolean completed,
    int newLevel,
    List<OutpostMaterialContributor> topContributors
) {
    public static OutpostDonateSuccess inProgress(
        int materialsFromItem,
        int deliveredAfter,
        int materialsRequired
    ) {
        return new OutpostDonateSuccess(materialsFromItem, deliveredAfter, materialsRequired, false, 0, List.of());
    }
}
