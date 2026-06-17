package ru.homyakin.seeker.game.outpost.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OutpostBuildingProgress(int materialsDelivered) {
    public static OutpostBuildingProgress started() {
        return new OutpostBuildingProgress(0);
    }
}
