package ru.homyakin.seeker.telegram.command.type;

import ru.homyakin.seeker.locale.feedback.FeedbackResource;
import ru.homyakin.seeker.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum FeedbackCommandType {
    SUGGEST_TEXT,
    OTHER_THEME,
    BACK,
    ;

    private static final Map<String, FeedbackCommandType> textToType = new HashMap<>();

    public static Optional<FeedbackCommandType> getFromString(String text) {
        return Optional.ofNullable(textToType.get(text));
    }

    public static void fillLocaleMap(FeedbackResource resource) {
        CommonUtils.putIfKeyPresents(textToType, resource.textButton(), FeedbackCommandType.SUGGEST_TEXT);
        CommonUtils.putIfKeyPresents(textToType, resource.errorButton(), FeedbackCommandType.OTHER_THEME);
        CommonUtils.putIfKeyPresents(textToType, resource.otherButton(), FeedbackCommandType.OTHER_THEME);
        CommonUtils.putIfKeyPresents(textToType, resource.improvementButton(), FeedbackCommandType.OTHER_THEME);
        CommonUtils.putIfKeyPresents(textToType, resource.backButton(), FeedbackCommandType.BACK);
    }
}
