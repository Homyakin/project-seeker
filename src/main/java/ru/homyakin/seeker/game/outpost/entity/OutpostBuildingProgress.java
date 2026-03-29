package ru.homyakin.seeker.game.outpost.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Progress of delivering materials until the building reaches the target level.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OutpostBuildingProgress(int materialsRequired, int materialsDelivered) {
    public static OutpostBuildingProgress started(int materialsRequired) {
        return new OutpostBuildingProgress(materialsRequired, 0);
    }
}
