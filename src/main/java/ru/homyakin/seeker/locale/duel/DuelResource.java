package ru.homyakin.seeker.locale.duel;

public record DuelResource(
    String duelMustBeReply,
    String[] duelWithDifferentBot,
    String[] duelWithThisBot,
    String duelWithYourself,
    String[] duelWithInitiatorNotEnoughMoney,
    String personageAlreadyStartDuel,
    String[] initDuel,
    String notDuelAcceptingPersonage,
    String[] expiredDuel,
    String[] declinedDuel,
    String[] finishedDuel,
    String acceptDuelButton,
    String declineDuelButton
) {
}
