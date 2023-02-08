package ru.homyakin.seeker.utils;

import java.nio.file.Path;
import java.util.Optional;

public class ResourceUtils {
    public static Optional<Path> getResourcePath(String path) {
       return Optional.ofNullable(ResourceUtils.class.getClassLoader().getResource(path))
            .map(it -> Path.of(it.getFile()));
    }
}
