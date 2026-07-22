package ru.homyakin.seeker.game.event.anomaly.entity;

public sealed interface AnomalyError {
    enum NotRegistered implements AnomalyError { INSTANCE }

    enum NoStormScanner implements AnomalyError { INSTANCE }

    enum AlreadyStartedToday implements AnomalyError { INSTANCE }

    enum ActiveAnomalyExists implements AnomalyError { INSTANCE }

    enum NotGroupMember implements AnomalyError { INSTANCE }

    enum EventNotFound implements AnomalyError { INSTANCE }

    enum InvalidPhase implements AnomalyError { INSTANCE }

    enum NotOwner implements AnomalyError { INSTANCE }

    enum PartyNotFull implements AnomalyError { INSTANCE }

    enum PartyEmpty implements AnomalyError { INSTANCE }

    enum RosterLocked implements AnomalyError { INSTANCE }

    enum AlreadyJoined implements AnomalyError { INSTANCE }

    enum PartyFull implements AnomalyError { INSTANCE }

    enum EventLocked implements AnomalyError { INSTANCE }

    enum FinalStatus implements AnomalyError { INSTANCE }
}
