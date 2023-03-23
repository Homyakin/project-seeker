package ru.homyakin.seeker.locale.duel;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.utils.CommonUtils;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.StringNamedTemplate;
import ru.homyakin.seeker.utils.models.Pair;

public class DuelLocalization {
    private static final Map<Language, DuelResource> map = new HashMap<>();

    public static void add(Language language, DuelResource resource) {
        map.put(language, resource);
    }

    public static String duelMustBeReply(Language language) {
        return CommonUtils.ifNullThan(map.get(language).duelMustBeReply(), map.get(Language.DEFAULT).duelMustBeReply());
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

    public static DuelText initDuel(Language language, Personage initiatingPersonage, Personage acceptingPersonage) {
        var text = RandomUtils.getRandomElement(
            CommonUtils.ifNullThan(map.get(language).initDuel(), map.get(Language.DEFAULT).initDuel())
        );
        final var positions = getPositionOfKeys(text, INITIATOR_KEY, ACCEPTOR_KEY);
        final var params = new HashMap<String, Object>();
        params.put(INITIATOR_KEY, initiatingPersonage.iconWithName());
        params.put(ACCEPTOR_KEY, acceptingPersonage.iconWithName());
        return new DuelText(
            StringNamedTemplate.format(text, params),
            positions.first(),
            positions.second()
        );
    }

    public static String notDuelAcceptingPersonage(Language language) {
        return CommonUtils.ifNullThan(map.get(language).notDuelAcceptingPersonage(), map.get(Language.DEFAULT).notDuelAcceptingPersonage());
    }

    public static String expiredDuel(Language language, Personage acceptingPersonage) {
        final var params = new HashMap<String, Object>();
        params.put("accepting_personage_icon_with_name", acceptingPersonage.iconWithName());
        return StringNamedTemplate.format(
            RandomUtils.getRandomElement(CommonUtils.ifNullThan(map.get(language).expiredDuel(), map.get(Language.DEFAULT).expiredDuel())),
            params
        );
    }

    public static String declinedDuel(Language language, Personage initiatingPersonage) {
        final var params = new HashMap<String, Object>();
        params.put("initiating_personage_icon_with_name", initiatingPersonage.iconWithName());
        return StringNamedTemplate.format(
            RandomUtils.getRandomElement(
                CommonUtils.ifNullThan(map.get(language).declinedDuel(), map.get(Language.DEFAULT).declinedDuel())),
            params
        );
    }

    public static EndDuelText finishedDuel(Language language, Personage winnerPersonage, Personage looserPersonage) {
        var text = RandomUtils.getRandomElement(
            CommonUtils.ifNullThan(map.get(language).finishedDuel(), map.get(Language.DEFAULT).finishedDuel())
        );
        final var positions = getPositionOfKeys(text, WINNER_KEY, LOOSER_KEY);
        final var params = new HashMap<String, Object>();
        params.put(WINNER_KEY, winnerPersonage.iconWithName());
        params.put(LOOSER_KEY, looserPersonage.iconWithName());
        return new EndDuelText(
            StringNamedTemplate.format(text, params),
            positions.first(),
            positions.second()
        );
    }

    public static String acceptDuelButton(Language language) {
        return CommonUtils.ifNullThan(map.get(language).acceptDuelButton(), map.get(Language.DEFAULT).acceptDuelButton());
    }

    public static String declineDuelButton(Language language) {
        return CommonUtils.ifNullThan(map.get(language).declineDuelButton(), map.get(Language.DEFAULT).declineDuelButton());
    }

    private static Pair<Optional<Integer>, Optional<Integer>> getPositionOfKeys(String text, String key1, String key2) {
        final var key1Index = text.indexOf(key1);
        final var key2Index = text.indexOf(key2);
        Optional<Integer> key1Position = Optional.empty();
        Optional<Integer> key2Position = Optional.empty();
        if (key1Index == -1 && key2Index != -1) {
            key2Position = Optional.of(1);
        } else if (key2Index == -1 && key1Index != -1) {
            key1Position = Optional.of(1);
        } else if (key1Index < key2Index) {
            key1Position = Optional.of(1);
            key2Position = Optional.of(2);
        } else if (key2Index < key1Index) {
            key2Position = Optional.of(1);
            key1Position = Optional.of(2);
        }

        return new Pair<>(key1Position, key2Position);
    }

    private static final String ACCEPTOR_KEY = "accepting_personage_icon_with_name";
    private static final String INITIATOR_KEY = "initiating_personage_icon_with_name";
    private static final String WINNER_KEY = "winner_personage_icon_with_name";
    private static final String LOOSER_KEY = "looser_personage_icon_with_name";
}
