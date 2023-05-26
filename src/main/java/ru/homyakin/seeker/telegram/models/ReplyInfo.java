package ru.homyakin.seeker.telegram.models;

import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Optional;

public record ReplyInfo(
    int messageId,
    long userId,
    MessageOwner messageOwner
) {
    public static Optional<ReplyInfo> from(Message message) {
        return Optional.ofNullable(message.getReplyToMessage()).map(
            it -> new ReplyInfo(it.getMessageId(), it.getFrom().getId(), MessageOwner.from(it))
        );
    }
}
