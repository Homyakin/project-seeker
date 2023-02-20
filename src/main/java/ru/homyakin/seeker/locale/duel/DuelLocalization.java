package ru.homyakin.seeker.locale.duel;

import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.game.personage.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.utils.CommonUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class DuelLocalization {
    private static final Map<Language, DuelResource> map = new HashMap<>();

    public static void add(Language language, DuelResource resource) {
        map.put(language, resource);
    }

    public static String duelMustBeReply(Language language) {
        return CommonUtils.ifNullThan(map.get(language).duelMustBeReply(), map.get(Language.DEFAULT).duelMustBeReply());
    }

    public static String duelReplyMustBeToUser(Language language) {
        return CommonUtils.ifNullThan(map.get(language).duelReplyMustBeToUser(), map.get(Language.DEFAULT).duelReplyMustBeToUser());
    }

    public static String duelWithYourself(Language language) {
        return CommonUtils.ifNullThan(map.get(language).duelWithYourself(), map.get(Language.DEFAULT).duelWithYourself());
    }

    public static String duelWithInitiatorNotEnoughMoney(Language language, Money money) {
        final var params = new HashMap<String, Object>() {{
            put("money_icon", TextConstants.MONEY_ICON);
            put("money_count", money.value());
        }};
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(
                map.get(language).duelWithInitiatorNotEnoughMoney(),
                map.get(Language.DEFAULT).duelWithInitiatorNotEnoughMoney()
            ),
            params
        );
    }

    public static String duelWithAcceptorNotEnoughMoney(Language language, Money money) {
        final var params = new HashMap<String, Object>() {{
            put("money_icon", TextConstants.MONEY_ICON);
            put("money_count", money.value());
        }};
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(
                map.get(language).duelWithAcceptorNotEnoughMoney(),
                map.get(Language.DEFAULT).duelWithAcceptorNotEnoughMoney()
            ),
            params
        );
    }

    public static String personageAlreadyStartDuel(Language language) {
        return CommonUtils.ifNullThan(map.get(language).personageAlreadyStartDuel(), map.get(Language.DEFAULT).personageAlreadyStartDuel());
    }

    public static String initDuel(Language language, Personage initiatingPersonage, Personage acceptingPersonage) {
        final var params = new HashMap<String, Object>() {{
            put("personage_icon", TextConstants.PERSONAGE_ICON);
            put("initiating_personage_name", initiatingPersonage.name());
            put("accepting_personage_name", acceptingPersonage.name());
        }};
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).initDuel(), map.get(Language.DEFAULT).initDuel()),
            params
        );
    }

    public static String notDuelAcceptingPersonage(Language language) {
        return CommonUtils.ifNullThan(map.get(language).notDuelAcceptingPersonage(), map.get(Language.DEFAULT).notDuelAcceptingPersonage());
    }

    public static String expiredDuel(Language language) {
        return CommonUtils.ifNullThan(map.get(language).expiredDuel(), map.get(Language.DEFAULT).expiredDuel());
    }

    public static String declinedDuel(Language language) {
        return CommonUtils.ifNullThan(map.get(language).declinedDuel(), map.get(Language.DEFAULT).declinedDuel());
    }

    public static String notEnoughMoneyAtAccepting(Language language, Money money) {
        final var params = new HashMap<String, Object>() {{
            put("money_icon", TextConstants.MONEY_ICON);
            put("money_count", money.value());
        }};
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).notEnoughMoneyAtAccepting(), map.get(Language.DEFAULT).notEnoughMoneyAtAccepting()),
            params
        );
    }

    public static String finishedDuel(Language language, Personage winnerPersonage, Personage looserPersonage) {
        final var params = new HashMap<String, Object>() {{
            put("personage_icon", TextConstants.PERSONAGE_ICON);
            put("winner_personage_name", winnerPersonage.name());
            put("looser_personage_name", looserPersonage.name());
        }};
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).finishedDuel(), map.get(Language.DEFAULT).finishedDuel()),
            params
        );
    }

    public static String acceptDuelButton(Language language) {
        return CommonUtils.ifNullThan(map.get(language).acceptDuelButton(), map.get(Language.DEFAULT).acceptDuelButton());
    }

    public static String declineDuelButton(Language language) {
        return CommonUtils.ifNullThan(map.get(language).declineDuelButton(), map.get(Language.DEFAULT).declineDuelButton());
    }
}
