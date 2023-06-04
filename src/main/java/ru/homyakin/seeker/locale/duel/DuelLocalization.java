package ru.homyakin.seeker.locale.duel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.infrastructure.PersonageMention;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.CommonUtils;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class DuelLocalization {
    private static final Map<Language, DuelResource> map = new HashMap<>();

    public static void add(Language language, DuelResource resource) {
        map.put(language, resource);
    }

    public static String duelMustContainsMention(Language language) {
        final Map<String, Object> param = Collections.singletonMap("duel_command", CommandType.START_DUEL.getText());
        return StringNamedTemplate.format(
            CommonUtils.ifNullThan(map.get(language).duelMustContainsMention(), map.get(Language.DEFAULT).duelMustContainsMention()),
            param
        );
    }

    public static String duelWithDifferentBot(Language language) {
        return RandomUtils.getRandomElement(
            CommonUtils.ifNullThan(map.get(language).duelWithDifferentBot(), map.get(Language.DEFAULT).duelWithDifferentBot())
        );
    }

    public static String duelWithThisBot(Language language) {
        return RandomUtils.getRandomElement(
            CommonUtils.ifNullThan(map.get(language).duelWithThisBot(), map.get(Language.DEFAULT).duelWithThisBot())
        );
    }

    public static String duelWithYourself(Language language) {
        return CommonUtils.ifNullThan(map.get(language).duelWithYourself(), map.get(Language.DEFAULT).duelWithYourself());
    }

    public static String duelWithInitiatorNotEnoughMoney(Language language, Money money) {
        final var params = new HashMap<String, Object>();
        params.put("money_icon", TextConstants.MONEY_ICON);
        params.put("money_count", money.value());
        return StringNamedTemplate.format(
            RandomUtils.getRandomElement(
                CommonUtils.ifNullThan(
                    map.get(language).duelWithInitiatorNotEnoughMoney(),
                    map.get(Language.DEFAULT).duelWithInitiatorNotEnoughMoney()
                )
            ),
            params
        );
    }

    public static String personageAlreadyStartDuel(Language language) {
        return CommonUtils.ifNullThan(map.get(language).personageAlreadyStartDuel(), map.get(Language.DEFAULT).personageAlreadyStartDuel());
    }

    public static String initDuel(Language language, PersonageMention initiatorMention, PersonageMention acceptorMention) {
        final var params = new HashMap<String, Object>();
        params.put("mention_initiator_icon_with_name", initiatorMention.value());
        params.put("mention_acceptor_icon_with_name", acceptorMention.value());
        return StringNamedTemplate.format(
            RandomUtils.getRandomElement(
                CommonUtils.ifNullThan(map.get(language).initDuel(), map.get(Language.DEFAULT).initDuel())
            ),
            params
        );
    }

    public static String notDuelAcceptingPersonage(Language language) {
        return CommonUtils.ifNullThan(map.get(language).notDuelAcceptingPersonage(), map.get(Language.DEFAULT).notDuelAcceptingPersonage());
    }

    public static String expiredDuel(Language language, PersonageMention acceptorMention) {
        final var params = new HashMap<String, Object>();
        params.put("mention_acceptor_icon_with_name", acceptorMention.value());
        return StringNamedTemplate.format(
            RandomUtils.getRandomElement(CommonUtils.ifNullThan(map.get(language).expiredDuel(), map.get(Language.DEFAULT).expiredDuel())),
            params
        );
    }

    public static String declinedDuel(Language language, PersonageMention initiatorMention) {
        final var params = new HashMap<String, Object>();
        params.put("mention_initiator_icon_with_name", initiatorMention.value());
        return StringNamedTemplate.format(
            RandomUtils.getRandomElement(
                CommonUtils.ifNullThan(map.get(language).declinedDuel(), map.get(Language.DEFAULT).declinedDuel())),
            params
        );
    }

    public static String finishedDuel(Language language, PersonageMention winnerMention, PersonageMention looserMention) {
        final var params = new HashMap<String, Object>();
        params.put("mention_winner_icon_with_name", winnerMention.value());
        params.put("mention_looser_icon_with_name", looserMention.value());
        return StringNamedTemplate.format(
            RandomUtils.getRandomElement(
                CommonUtils.ifNullThan(map.get(language).finishedDuel(), map.get(Language.DEFAULT).finishedDuel())
            ),
            params
        );
    }

    public static String acceptDuelButton(Language language) {
        return CommonUtils.ifNullThan(map.get(language).acceptDuelButton(), map.get(Language.DEFAULT).acceptDuelButton());
    }

    public static String declineDuelButton(Language language) {
        return CommonUtils.ifNullThan(map.get(language).declineDuelButton(), map.get(Language.DEFAULT).declineDuelButton());
    }

    public static String duelWithUnknownUser(Language language) {
        return RandomUtils.getRandomElement(
            CommonUtils.ifNullThan(map.get(language).duelWithUnknownUser(), map.get(Language.DEFAULT).duelWithUnknownUser())
        );
    }
}
