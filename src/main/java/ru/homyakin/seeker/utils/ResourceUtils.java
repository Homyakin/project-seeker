package ru.homyakin.seeker.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;

public class ResourceUtils {
    public static Optional<InputStream> getResourcePath(String path) {
        try {
            return Optional.of(new ClassPathResource(path).getInputStream());
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
