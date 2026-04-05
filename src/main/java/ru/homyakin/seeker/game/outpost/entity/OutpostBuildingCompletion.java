package ru.homyakin.seeker.game.outpost.entity;

import java.util.List;

public record OutpostBuildingCompletion(
    Building building,
    int newLevel,
    List<OutpostMaterialContributor> topContributors
) {
}
