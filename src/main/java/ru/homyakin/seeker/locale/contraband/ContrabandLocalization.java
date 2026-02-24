package ru.homyakin.seeker.locale.contraband;

import ru.homyakin.seeker.game.contraband.entity.Contraband;
import ru.homyakin.seeker.game.contraband.entity.ContrabandOpenResult;
import ru.homyakin.seeker.game.contraband.entity.ContrabandTier;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.utils.StringNamedTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class ContrabandLocalization {
    private static final Resources<ContrabandResource> resources = new Resources<>();

    public static void add(Language language, ContrabandResource resource) {
        resources.add(language, resource);
    }

    public static String contrabandName(Language language, ContrabandTier tier) {
        return switch (tier) {
            case COMMON -> resources.getOrDefault(language, ContrabandResource::commonChestName);
            case RARE -> resources.getOrDefault(language, ContrabandResource::rareChestName);
            case EPIC -> resources.getOrDefault(language, ContrabandResource::epicChestName);
        };
    }

    public static String contrabandDisplayForReport(Language language, Contraband contraband) {
        return contrabandName(language, contraband.tier());
    }

    public static String contrabandFoundPrivateMessage(
        Language language, Contraband contraband, int finderChance, Money sellPrice
    ) {
        final var params = new HashMap<String, Object>();
        params.put("contraband_name", contrabandName(language, contraband.tier()));
        params.put("finder_chance", finderChance);
        params.put("money_icon", Icons.MONEY);
        params.put("sell_price", sellPrice.value());
        final var timeLeft = Duration.between(LocalDateTime.now(), contraband.expiresAt());
        params.put("time_left", CommonLocalization.duration(language, timeLeft));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ContrabandResource::contrabandFoundPrivateMessage),
            params
        );
    }

    public static String forceOpenButton(Language language, int chance) {
        final var params = new HashMap<String, Object>();
        params.put("chance", chance);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ContrabandResource::forceOpenButton),
            params
        );
    }

    public static String sellToMarketButton(Language language, Money price) {
        final var params = new HashMap<String, Object>();
        params.put("money_icon", Icons.MONEY);
        params.put("price", price.value());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ContrabandResource::sellToMarketButton),
            params
        );
    }

    public static String openAsReceiverButton(Language language, int chance) {
        final var params = new HashMap<String, Object>();
        params.put("chance", chance);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ContrabandResource::openAsReceiverButton),
            params
        );
    }

    public static String openResult(Language language, ContrabandOpenResult result) {
        return switch (result) {
            case ContrabandOpenResult.Success success -> {
                final var reward = formatReward(language, success);
                yield StringNamedTemplate.format(
                    resources.getOrDefaultRandom(language, ContrabandResource::openSuccess),
                    Map.of("rewards", reward)
                );
            }
            case ContrabandOpenResult.Failure failure -> {
                final var penalty = formatPenalty(language, failure);
                yield StringNamedTemplate.format(
                    resources.getOrDefaultRandom(language, ContrabandResource::openFailure),
                    Map.of("penalty", penalty)
                );
            }
        };
    }

    private static String formatPenalty(Language language, ContrabandOpenResult.Failure failure) {
        return switch (failure) {
            case ContrabandOpenResult.Failure.Debuff debuff ->
                CommonLocalization.contrabandEffect(language, debuff.effect());
            case ContrabandOpenResult.Failure.GoldLoss loss -> StringNamedTemplate.format(
                resources.getOrDefault(language, ContrabandResource::penaltyGoldLoss),
                Map.of("money_icon", Icons.MONEY, "amount", loss.amount())
            );
            case ContrabandOpenResult.Failure.Nothing _ ->
                resources.getOrDefault(language, ContrabandResource::penaltyNothing);
        };
    }

    public static String sellSuccess(Language language, Money sellPrice) {
        final var params = new HashMap<String, Object>();
        params.put("money_icon", Icons.MONEY);
        params.put("sell_price", sellPrice.value());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ContrabandResource::sellSuccess),
            params
        );
    }

    public static String receiverNotification(
        Language language, Contraband contraband, Personage finder, int receiverChance
    ) {
        final var params = new HashMap<String, Object>();
        params.put("contraband_name", contrabandName(language, contraband.tier()));
        params.put("finder", LocaleUtils.personageNameWithBadge(finder));
        params.put("receiver_chance", receiverChance);
        final var timeLeft = Duration.between(LocalDateTime.now(), contraband.expiresAt());
        params.put("time_left", CommonLocalization.duration(language, timeLeft));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ContrabandResource::receiverNotification),
            params
        );
    }

    public static String echoToFinderSuccess(Language language, Contraband contraband, Personage receiver) {
        final var params = new HashMap<String, Object>();
        params.put("contraband_name", contrabandName(language, contraband.tier()));
        params.put("receiver", LocaleUtils.personageNameWithBadge(receiver));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ContrabandResource::echoToFinderSuccess),
            params
        );
    }

    public static String echoToFinderFailure(Language language, Contraband contraband, Personage receiver) {
        final var params = new HashMap<String, Object>();
        params.put("contraband_name", contrabandName(language, contraband.tier()));
        params.put("receiver", LocaleUtils.personageNameWithBadge(receiver));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ContrabandResource::echoToFinderFailure),
            params
        );
    }

    public static String contrabandExpired(Language language, ContrabandTier tier) {
        final var params = new HashMap<String, Object>();
        params.put("contraband_name", contrabandName(language, tier));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ContrabandResource::contrabandExpired),
            params
        );
    }

    private static String formatReward(Language language, ContrabandOpenResult.Success success) {
        return switch (success) {
            case ContrabandOpenResult.Success.Gold gold -> StringNamedTemplate.format(
                resources.getOrDefault(language, ContrabandResource::rewardGold),
                Map.of("money_icon", Icons.MONEY, "amount", gold.amount().value())
            );
            case ContrabandOpenResult.Success.ItemReward item -> StringNamedTemplate.format(
                resources.getOrDefault(language, ContrabandResource::rewardItem),
                Map.of("item_name", ItemLocalization.shortItem(language, item.item()))
            );
            case ContrabandOpenResult.Success.Energy energy -> StringNamedTemplate.format(
                resources.getOrDefault(language, ContrabandResource::rewardEnergy),
                Map.of("amount", energy.amount(), "energy_icon", Icons.ENERGY)
            );
            case ContrabandOpenResult.Success.Buff buff ->
                CommonLocalization.contrabandEffect(language, buff.effect());
        };
    }

    public static String contrabandAlreadyProcessed(Language language) {
        return resources.getOrDefault(language, ContrabandResource::contrabandAlreadyProcessed);
    }

    public static String contrabandExpiredError(Language language) {
        return resources.getOrDefault(language, ContrabandResource::contrabandExpiredError);
    }

    public static String noActiveContraband(Language language) {
        return resources.getOrDefault(language, ContrabandResource::noActiveContraband);
    }
}
