package ru.homyakin.seeker.telegram.models;

public sealed interface ChatMemberError {

    enum UserNotFound implements ChatMemberError { INSTANCE }

    enum InvalidParticipant implements ChatMemberError { INSTANCE }

    record InternalError(String message) implements ChatMemberError {}

}
