package ru.homyakin.seeker.utils;

import java.nio.file.Path;
import java.util.Objects;

public class ResourceUtils {
    public static Path getResourcePath(String path) {
        final var resource = ResourceUtils.class.getClassLoader().getResource(path);
        Objects.requireNonNull(resource);
        return Path.of(resource.getFile());
    }
}
