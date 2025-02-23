package ru.homyakin.seeker.locale.feedback;

import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.telegram.command.user.feedback.InputFeedback;
import ru.homyakin.seeker.utils.StringNamedTemplate;

import java.util.HashMap;

public class FeedbackLocalization {
    private static final Resources<FeedbackResource> resources = new Resources<>();

    public static void add(Language language, FeedbackResource resource) {
        resources.add(language, resource);
    }

    public static String initFeedback(Language language) {
        return resources.getOrDefault(language, FeedbackResource::initFeedback);
    }

    public static String errorButton(Language language) {
        return resources.getOrDefault(language, FeedbackResource::errorButton);
    }

    public static String improvementButton(Language language) {
        return resources.getOrDefault(language, FeedbackResource::improvementButton);
    }

    public static String textButton(Language language) {
        return resources.getOrDefault(language, FeedbackResource::textButton);
    }

    public static String otherButton(Language language) {
        return resources.getOrDefault(language, FeedbackResource::otherButton);
    }

    public static String inputTextSuggestion(Language language) {
        return resources.getOrDefault(language, FeedbackResource::inputTextSuggestion);
    }

    public static String inputFeedback(Language language) {
        return resources.getOrDefault(language, FeedbackResource::inputFeedback);
    }

    public static String cancelFeedback(Language language) {
        return resources.getOrDefault(language, FeedbackResource::cancelFeedback);
    }

    public static String invalidTheme(Language language) {
        return resources.getOrDefault(language, FeedbackResource::invalidTheme);
    }

    public static String feedback(Language language, Personage personage, InputFeedback inputFeedback) {
        final var params = new HashMap<String, Object>();
        params.put("personage_badge_with_name", LocaleUtils.personageNameWithBadge(personage));
        params.put("personage_id", personage.id().value());
        params.put("feedback_theme", inputFeedback.theme());
        params.put("feedback_text", inputFeedback.feedback());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, FeedbackResource::feedback),
            params
        );
    }

    public static String feedbackSent(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("feedback_channel", TextConstants.FEEDBACK_TG_CHANNEL);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, FeedbackResource::feedbackSent),
            params
        );
    }

    public static String feedbackErrorSent(Language language) {
        return resources.getOrDefault(language, FeedbackResource::feedbackErrorSent);
    }
}
