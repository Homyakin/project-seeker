package ru.homyakin.seeker.game.personage.models.errors;

public sealed interface PersonageEventError permits EventNotExist, ExpiredEvent, PersonageInOtherEvent, PersonageInThisEvent {
}
