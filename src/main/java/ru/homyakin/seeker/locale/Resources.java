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

    /**
     * @param language Запрашиваемый язык. Если в resources нет нужного языка, возвращает <code>Language.DEFAULT</code>
     * @param function Функция, которая возвращает нужную строку. Например, <code>CommonResource::fullProfile</code>
     *                 вернёт строку для полного профиля
     */
    public String getOrDefault(Language language, Function<T, String> function) {
        return getOrDefaultTemplate(language, function);
    }

    /**
     * @param language Запрашиваемый язык. Если в resources нет нужного языка, возвращает <code>Language.DEFAULT</code>
     * @param function Функция, которая возвращает нужный массив из возможных строк. Например, <code>DuelResource::finishedDuel</code>
     *                 возвращает массив возможных вариантов конца дуэли
     */
    public String getOrDefaultRandom(Language language, Function<T, String[]> function) {
        return RandomUtils.getRandomElement(getOrDefaultTemplate(language, function));
    }

    /**
     * @param language Запрашиваемый язык. Если в resources нет нужного языка, возвращает <code>Language.DEFAULT</code>
     * @param function Функция, которая возвращает нужный какое-то значение из ресурса. Считается, что в resources всегда
     *                 присутствует <code>Language.DEFAULT</code>
     */
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
