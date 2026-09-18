package ru.homyakin.seeker.game.item.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;

/**
 * Loads an equipment release into memory without invoking application startup or database code.
 */
public final class EquipmentCatalogLoader {
    private EquipmentCatalogLoader() {
    }

    public static LoadedEquipmentCatalog load(EquipmentCatalogVersion version) {
        return load(EquipmentCatalogRelease.forVersion(version));
    }

    public static LoadedEquipmentCatalog loadValidated(EquipmentCatalogVersion version) {
        return loadValidated(EquipmentCatalogRelease.forVersion(version));
    }

    public static LoadedEquipmentCatalog loadValidated(EquipmentCatalogRelease release) {
        final var catalog = load(release);
        EquipmentCatalogValidator.validate(catalog);
        return catalog;
    }

    public static LoadedEquipmentCatalog load(EquipmentCatalogRelease release) {
        if (release == null) {
            throw new IllegalArgumentException("Catalog release must be specified");
        }
        final var itemObjects = loadResource(
            release.itemObjectsPath(),
            stream -> ItemObjectsToml.load(stream).itemObjects()
        );
        final var modifiers = loadResource(
            release.itemModifiersPath(),
            stream -> ItemModifiersToml.load(stream).modifiers()
        );
        final var defaultItems = loadResource(
            release.defaultItemsPath(),
            stream -> ItemObjectsToml.load(stream).itemObjects()
        );
        return new LoadedEquipmentCatalog(release, itemObjects, modifiers, defaultItems);
    }

    private static <T> T loadResource(String path, Function<InputStream, T> loader) {
        final var classLoader = EquipmentCatalogLoader.class.getClassLoader();
        final var resource = classLoader.getResourceAsStream(path);
        if (resource == null) {
            throw new IllegalStateException("Equipment catalog resource is missing: " + path);
        }
        try (resource) {
            return loader.apply(resource);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to close equipment catalog resource: " + path, e);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to load equipment catalog resource: " + path, e);
        }
    }
}
