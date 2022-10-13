package ru.homyakin.seeker.game.personage.model.error;

public sealed interface PersonageEventError permits EventNotExist, ExpiredEvent, PersonageInOtherEvent, PersonageInThisEvent {
}
