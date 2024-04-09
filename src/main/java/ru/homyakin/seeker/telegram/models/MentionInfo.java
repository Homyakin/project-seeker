package ru.homyakin.seeker.telegram.models;

import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Optional;
import ru.homyakin.seeker.telegram.user.models.UserId;

public sealed interface MentionInfo {
    UserType userType();

    record Id(UserId userId, UserType userType) implements MentionInfo {
        public static Id from(User user) {
            return new Id(UserId.from(user.getId()), UserType.from(user));
        }

        private static final String TEXT_MENTION_ENTITY = "text_mention";
    }

    record Username(String username, UserType userType) implements MentionInfo {
        public static Username from(String username) {
            return new Username(username, UserType.from(username));
        }

        private static final String MENTION_ENTITY = "mention";
    }

    static Optional<MentionInfo> from(Message message) {
        if (message.hasEntities()) {
            for (final var entity: message.getEntities()) {
                if (entity.getType().equals(Username.MENTION_ENTITY)) {
                    return Optional.of(
                        Username.from(entity.getText().replace("@", ""))
                    );
                } else if (entity.getType().equals(Id.TEXT_MENTION_ENTITY)) {
                    return Optional.of(Id.from(entity.getUser()));
                }
            }
        }
        return Optional.ofNullable(message.getReplyToMessage()).map(it -> Id.from(it.getFrom()));
    }
}
