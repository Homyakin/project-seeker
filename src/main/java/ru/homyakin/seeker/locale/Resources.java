package ru.homyakin.seeker.locale;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import ru.homyakin.seeker.utils.RandomUtils;

public class Resources<T> {
    private final Map<Language, T> resources = new HashMap<>();

    public void add(Language language, T resource) {
        resources.put(language, resource);
    }

    public String getOrDefault(Language language, Function<T, String> function) {
        return getOrDefaultTemplate(language, function);
    }

    public String getOrDefaultRandom(Language language, Function<T, String[]> function) {
        return RandomUtils.getRandomElement(getOrDefaultTemplate(language, function));
    }

    private <K> K getOrDefaultTemplate(Language language, Function<T, K> function) {
        final var resource = resources.get(language);
        if (resource != null) {
            final var result = function.apply(resource);
            if (result != null) {
                return result;
            }
        }
        return function.apply(resources.get(Language.DEFAULT));
    }
}
