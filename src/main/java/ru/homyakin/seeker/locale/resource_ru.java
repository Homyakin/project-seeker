package ru.homyakin.seeker.locale;

public class resource_ru extends AbstractResource {
    private static final Object[][] contents =
        {
            {
                LocalizationKeys.WELCOME.value,
                """
                Приветствую вас, Искатели.
                """
            }
        };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
