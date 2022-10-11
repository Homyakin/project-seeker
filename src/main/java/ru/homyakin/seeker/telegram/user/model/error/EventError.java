package ru.homyakin.seeker.telegram.user.model.error;

public sealed interface EventError permits EventNotExist, ExpiredEvent, UserInOtherEvent, UserInThisEvent {
}
