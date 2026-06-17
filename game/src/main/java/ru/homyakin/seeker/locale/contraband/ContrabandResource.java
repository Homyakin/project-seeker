package ru.homyakin.seeker.locale.contraband;

public record ContrabandResource(
    String contrabandFoundPrivateMessage,
    String commonChestName,
    String uncommonChestName,
    String rareChestName,
    String epicChestName,
    String forceOpenButton,
    String sellToMarketButton,
    String openAsReceiverButton,
    String[] openSuccess,
    String[] openFailure,
    String penaltyGoldLoss,
    String penaltyNothing,
    String sellSuccess,
    String receiverNotification,
    String echoToFinderSuccess,
    String echoToFinderFailure,
    String contrabandExpired,
    String rewardGold,
    String rewardItem,
    String rewardEnergy,
    String contrabandAlreadyProcessed,
    String contrabandExpiredError,
    String noActiveContraband
) {
}
