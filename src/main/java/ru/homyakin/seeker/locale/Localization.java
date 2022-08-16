package ru.homyakin.seeker.locale;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import ru.homyakin.seeker.models.Language;

public class Localization {
    private static final Map<Language, AbstractResource> map = new HashMap<>() {{
        put(Language.RU, (AbstractResource) ResourceBundle.getBundle(AbstractResource.BASE_NAME, new Locale(Language.RU.value())));
        put(Language.EN, (AbstractResource) ResourceBundle.getBundle(AbstractResource.BASE_NAME, new Locale(Language.EN.value())));
    }};

    public static AbstractResource get(Language language) {
        return map.get(language);
    }
}
