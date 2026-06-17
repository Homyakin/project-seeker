package ru.homyakin.seeker.telegram.command.common.badge;

import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import ru.homyakin.seeker.game.badge.entity.ActivateBadgeError;
import ru.homyakin.seeker.game.badge.entity.AvailableBadge;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.personal.BadgeLocalization;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

import java.util.List;
import java.util.Optional;

public class BadgeUtils {
    public static SendMessage showBadges(
        UserId userId,
        Language language,
        List<AvailableBadge> badges
    ) {
        return showBadges(userId.value(), language, badges);
    }

    public static SendMessage showBadges(
        GroupTgId groupTgId,
        Language language,
        List<AvailableBadge> badges
    ) {
        return showBadges(groupTgId.value(), language, badges);
    }

    private static SendMessage showBadges(
        long chatId,
        Language language,
        List<AvailableBadge> badges
    ) {
        return SendMessageBuilder.builder()
            .chatId(chatId)
            .text(BadgeLocalization.availableBadges(language, badges))
            .keyboard(InlineKeyboards.badgeSelector(badges))
            .build();
    }

    public static EditMessageText editBadges(
        UserId userId,
        int messageId,
        Language language,
        List<AvailableBadge> badges
    ) {
        return editBadges(userId.value(), messageId, language, badges);
    }

    public static EditMessageText editBadges(
        GroupTgId groupTgId,
        int messageId,
        Language language,
        List<AvailableBadge> badges
    ) {
        return editBadges(groupTgId.value(), messageId, language, badges);
    }

    private static EditMessageText editBadges(
        long chatId,
        int messageId,
        Language language,
        List<AvailableBadge> badges
    ) {
        return EditMessageTextBuilder.builder()
            .chatId(chatId)
            .text(BadgeLocalization.availableBadges(language, badges))
            .keyboard(InlineKeyboards.badgeSelector(badges))
            .messageId(messageId)
            .build();
    }

    public static Optional<AnswerCallbackQuery> processSelectError(
        ActivateBadgeError error,
        String callbackId,
        Language language
    ) {
        return Optional.ofNullable(switch (error) {
            case ActivateBadgeError.BadgeIsNotAvailable _ -> TelegramMethods.createAnswerCallbackQuery(
                callbackId,
                BadgeLocalization.badgeIsNotAvailable(language)
            );
            case ActivateBadgeError.AlreadyActivated _ -> null;
        });
    }
}
