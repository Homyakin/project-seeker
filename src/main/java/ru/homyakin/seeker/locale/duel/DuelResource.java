package ru.homyakin.seeker.locale.duel;

public record DuelResource(
    String duelMustBeReply,
    String duelReplyMustBeToUser,
    String duelWithYourself,
    String duelWithInitiatorLowHealth,
    String duelWithAcceptorLowHealth,
    String personageAlreadyStartDuel,
    String initDuel,
    String notDuelAcceptingPersonage,
    String expiredDuel,
    String declinedDuel,
    String finishedDuel,
    String acceptDuelButton,
    String declineDuelButton
) {
}
