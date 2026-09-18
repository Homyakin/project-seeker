package ru.homyakin.seeker.game.item.models;

public record CatalogModifier(
    int id,
    Modifier modifier,
    boolean assignmentEnabled
) {
    public CatalogModifier(int id, Modifier modifier) {
        this(id, modifier, true);
    }
}
