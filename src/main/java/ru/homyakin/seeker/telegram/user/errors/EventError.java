package ru.homyakin.seeker.telegram.user.errors;

public sealed interface EventError permits EventNotExist, ExpiredEvent, UserInOtherEvent, UserInThisEvent {
}
