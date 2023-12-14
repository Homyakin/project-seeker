package ru.homyakin.seeker.game.personage.models.errors;

// TODO перенести в класс
public sealed interface PersonageEventError permits EventNotExist, ExpiredEvent, PersonageEventError.EventInProcess,
    PersonageInOtherEvent, PersonageInThisEvent {
    enum EventInProcess implements PersonageEventError { INSTANCE }
}
