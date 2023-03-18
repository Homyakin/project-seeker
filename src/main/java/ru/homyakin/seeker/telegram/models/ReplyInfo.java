package ru.homyakin.seeker.telegram.models;

public record ReplyInfo(
    int messageId,
    long userId,
    MessageOwner messageOwner
) {
}
