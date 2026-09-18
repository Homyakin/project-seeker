package ru.homyakin.seeker.game.item.models;

public record CatalogItemObject(
    int id,
    ItemObject object,
    boolean acquisitionEnabled
) {
    public CatalogItemObject(int id, ItemObject object) {
        this(id, object, true);
    }
}
