package ru.homyakin.seeker.locale.duel;

import java.util.HashMap;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.utils.StringNamedTemplate;

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
    public String finishedDuel(Personage winnerPersonage, Personage looserPersonage) {
        final var params = new HashMap<String, Object>() {{
            put("level_icon", TextConstants.LEVEL_ICON);
            put("winner_personage_name", winnerPersonage.name());
            put("looser_personage_name", looserPersonage.name());
        }};
        return StringNamedTemplate.format(
            finishedDuel,
            params
        );
    }

    public String initDuel(Personage initiatingPersonage, Personage acceptingPersonage) {
        final var params = new HashMap<String, Object>() {{
            put("level_icon", TextConstants.LEVEL_ICON);
            put("health_icon", TextConstants.HEALTH_ICON);
            put("initiating_personage_name", initiatingPersonage.name());
            put("initiating_personage_health", initiatingPersonage.health());
            put("accepting_personage_name", acceptingPersonage.name());
            put("accepting_personage_health", acceptingPersonage.health());
        }};
        return StringNamedTemplate.format(
            initDuel,
            params
        );
    }
}
