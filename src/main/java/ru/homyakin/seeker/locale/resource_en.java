package ru.homyakin.seeker.locale;

public class resource_en extends AbstractResource {
    private static final Object[][] contents =
        {
            {
                LocalizationKeys.WELCOME.value,
                """
                Greetings, Seekers.
                """
            }
        };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
