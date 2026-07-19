package ru.homyakin.seeker.game.outpost.entity;

import java.util.List;

public record OutpostDonateSuccess(
    int materialsFromItem,
    int deliveredAfter,
    int materialsRequired,
    boolean completed,
    int newLevel,
    List<OutpostContributor> topContributors,
    List<String> affectedLoadoutNames
) {
    public static OutpostDonateSuccess inProgress(
        int materialsFromItem,
        int deliveredAfter,
        int materialsRequired,
        List<String> affectedLoadoutNames
    ) {
        return new OutpostDonateSuccess(
            materialsFromItem,
            deliveredAfter,
            materialsRequired,
            false,
            0,
            List.of(),
            affectedLoadoutNames
        );
    }
}
