package ru.homyakin.seeker.game.contraband.entity;

public sealed interface ReceiverContrabandError {
    enum NotFound implements ReceiverContrabandError { INSTANCE }

    enum NotReceiver implements ReceiverContrabandError { INSTANCE }

    enum Expired implements ReceiverContrabandError { INSTANCE }
}
