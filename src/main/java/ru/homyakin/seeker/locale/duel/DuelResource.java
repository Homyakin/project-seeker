package ru.homyakin.seeker.locale.duel;

public record DuelResource(
    String duelMustBeReply,
    String duelReplyMustBeToUser,
    String duelWithYourself,
    String duelWithInitiatorNotEnoughMoney,
    String duelWithAcceptorNotEnoughMoney,
    String personageAlreadyStartDuel,
    String initDuel,
    String notDuelAcceptingPersonage,
    String expiredDuel,
    String declinedDuel,
    String notEnoughMoneyAtAccepting,
    String finishedDuel,
    String acceptDuelButton,
    String declineDuelButton
) {
}
