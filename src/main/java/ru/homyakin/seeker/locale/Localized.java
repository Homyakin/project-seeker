package ru.homyakin.seeker.locale;

import java.util.List;

public interface Localized<T extends LanguageObject> {
    List<T> locales();
}
