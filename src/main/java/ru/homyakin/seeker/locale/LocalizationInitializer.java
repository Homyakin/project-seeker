package ru.homyakin.seeker.locale;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tomlj.Toml;
import ru.homyakin.seeker.telegram.command.CommandType;
import ru.homyakin.seeker.utils.ResourceUtils;

public class LocalizationInitializer {
    private static final String LOCALIZATION_PATH = "localization/";
    private static final Logger logger = LoggerFactory.getLogger(LocalizationInitializer.class);

    public static void initLocale() throws IOException {
        logger.info("Filling localization");
        final var commonRuMap = Toml.parse(ResourceUtils.getResourcePath(LOCALIZATION_PATH + "ru/common.toml")).toMap();
        final var resourceRu = new CommonResource(commonRuMap);

        final var commonEnMap = Toml.parse(ResourceUtils.getResourcePath(LOCALIZATION_PATH + "en/common.toml")).toMap();
        final var resourceEn = new CommonResource(commonEnMap);

        Localization.add(Language.RU, resourceRu);
        Localization.add(Language.EN, resourceEn);

        CommandType.fillLocaleMap(resourceRu);
        CommandType.fillLocaleMap(resourceEn);
        logger.info("Localization loaded");
    }
}
