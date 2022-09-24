package ru.homyakin.seeker.user.errors;

public sealed interface EventError permits EventNotExist, ExpiredEvent, UserInOtherEvent, UserInThisEvent {
}
