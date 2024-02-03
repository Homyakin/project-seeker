package ru.homyakin.seeker.locale.duel;

import java.util.Collections;
import java.util.HashMap;
import ru.homyakin.seeker.game.battle.PersonageBattleResult;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.infrastructure.PersonageMention;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class DuelLocalization {
    private static final Resources<DuelResource> resources = new Resources<>();

    public static void add(Language language, DuelResource resource) {
        resources.add(language, resource);
    }

    public static String duelMustContainsMention(Language language) {
        final var param = Collections.<String, Object>singletonMap("duel_command", CommandType.START_DUEL.getText());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, DuelResource::duelMustContainsMention),
            param
        );
    }

    public static String duelWithDifferentBot(Language language) {
        return resources.getOrDefaultRandom(language, DuelResource::duelWithDifferentBot);
    }

    public static String duelWithThisBot(Language language) {
        return resources.getOrDefaultRandom(language, DuelResource::duelWithThisBot);
    }

    public static String duelWithYourself(Language language) {
        return resources.getOrDefault(language, DuelResource::duelWithYourself);
    }

    public static String duelWithInitiatorNotEnoughMoney(Language language, Money money) {
        final var params = new HashMap<String, Object>();
        params.put("money_icon", Icons.MONEY);
        params.put("money_count", money.value());
        return StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, DuelResource::duelWithInitiatorNotEnoughMoney),
            params
        );
    }

    public static String personageAlreadyStartDuel(Language language) {
        return resources.getOrDefault(language, DuelResource::personageAlreadyStartDuel);
    }

    public static String initDuel(Language language, PersonageMention initiatorMention, PersonageMention acceptorMention) {
        final var params = new HashMap<String, Object>();
        params.put("mention_initiator_icon_with_name", initiatorMention.value());
        params.put("mention_acceptor_icon_with_name", acceptorMention.value());
        return StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, DuelResource::initDuel),
            params
        );
    }

    public static String notDuelAcceptingPersonage(Language language) {
        return resources.getOrDefault(language, DuelResource::notDuelAcceptingPersonage);
    }

    public static String expiredDuel(Language language, PersonageMention acceptorMention) {
        final var params = new HashMap<String, Object>();
        params.put("mention_acceptor_icon_with_name", acceptorMention.value());
        return StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, DuelResource::expiredDuel),
            params
        );
    }

    public static String declinedDuel(Language language, PersonageMention initiatorMention) {
        final var params = new HashMap<String, Object>();
        params.put("mention_initiator_icon_with_name", initiatorMention.value());
        return StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, DuelResource::declinedDuel),
            params
        );
    }

    public static String finishedDuel(Language language, PersonageMention winnerMention, PersonageMention loserMention) {
        final var params = new HashMap<String, Object>();
        params.put("mention_winner_icon_with_name", winnerMention.value());
        params.put("mention_loser_icon_with_name", loserMention.value());
        return StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, DuelResource::finishedDuel),
            params
        );
    }

    public static String acceptDuelButton(Language language) {
        return resources.getOrDefault(language, DuelResource::acceptDuelButton);
    }

    public static String declineDuelButton(Language language) {
        return resources.getOrDefault(language, DuelResource::declineDuelButton);
    }

    public static String duelWithUnknownUser(Language language) {
        return resources.getOrDefaultRandom(language, DuelResource::duelWithUnknownUser);
    }

    public static String duelIsLocked(Language language) {
        return resources.getOrDefault(language, DuelResource::duelIsLocked);
    }

    public static String personageDuelResult(Language language, PersonageBattleResult result, boolean isWinner) {
        final var params = new HashMap<String, Object>();
        params.put("winner_or_loser_icon", isWinner ? Icons.DUEL_WINNER : Icons.DUEL_LOSER);
        params.put("personage_badge_with_name", result.personage().iconWithName());
        params.put("damage_dealt", result.stats().damageDealt());
        params.put("damage_taken", result.stats().damageTaken());
        params.put("crits_count", result.stats().critsCount());
        params.put("dodges_count", result.stats().dodgesCount());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, DuelResource::personageDuelResult),
            params
        );
    }

    public static String duelAlreadyFinished(Language language) {
        return resources.getOrDefault(language, DuelResource::duelAlreadyFinished);
    }
}
