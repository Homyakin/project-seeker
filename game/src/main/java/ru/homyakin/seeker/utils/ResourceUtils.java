package ru.homyakin.seeker.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

public class ResourceUtils {
    private static final Logger logger = LoggerFactory.getLogger(ResourceUtils.class);

    public static void doAction(String path, Consumer<InputStream> action) {
        try (final var stream = new ClassPathResource(path).getInputStream()) {
            action.accept(stream);
        } catch (IOException _) {
        }
    }

    public static <T> Optional<T> calc(String path, Function<InputStream, T> function) {
        try (final var stream = new ClassPathResource(path).getInputStream()) {
            return Optional.of(function.apply(stream));
        } catch (IOException _) {
            return Optional.empty();
        }
    }

    public static List<Path> listAllFiles(String path) {
        try {
            final var directory = Paths.get(new ClassPathResource(path).getURI());
            try (final var list = Files.list(directory)) {
                return list.filter(Files::isRegularFile).toList();
            }
        } catch (IOException e) {
            logger.error("Error during listing resource " + path, e);
            return List.of();
        }
    }
}
