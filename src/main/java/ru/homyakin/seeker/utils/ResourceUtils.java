package ru.homyakin.seeker.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import org.springframework.core.io.ClassPathResource;

public class ResourceUtils {
    public static void doAction(String path, Consumer<InputStream> action) {
        try (final var stream = new ClassPathResource(path).getInputStream()) {
            action.accept(stream);
        } catch (IOException ignored) {
        }
    }
}
