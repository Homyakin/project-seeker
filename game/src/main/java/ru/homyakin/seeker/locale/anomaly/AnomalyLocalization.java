package ru.homyakin.seeker.locale.anomaly;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;
import ru.homyakin.seeker.game.event.anomaly.entity.Anomaly;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyError;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyMode;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.event.EventParticipant;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;
import ru.homyakin.seeker.utils.TimeUtils;

public final class AnomalyLocalization {
    private static final Resources<AnomalyResource> resources = new Resources<>();

    private AnomalyLocalization() {
    }

    public static void add(Language language, AnomalyResource resource) {
        resources.add(language, resource);
    }

    public static String searchButton(Language language) {
        return resources.getOrDefault(language, AnomalyResource::anomalySearchButton);
    }

    public static String menu(Language language, boolean canStart, boolean isRegistered) {
        final var params = new HashMap<String, Object>();
        params.put("starts_left", canStart ? 1 : 0);
        params.put("register_warning", isRegistered ? "" : registerWarning(language));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, AnomalyResource::anomalyMenu),
            params
        );
    }

    private static String registerWarning(Language language) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, AnomalyResource::anomalyMenuRegisterWarning),
            Map.of("register_group_command", CommandType.REGISTER_GROUP.getText())
        );
    }

    public static String discovered(Language language) {
        return resources.getOrDefault(language, AnomalyResource::anomalyDiscovered);
    }

    public static String gathering(
        Language language,
        Anomaly anomaly,
        List<EventParticipant> participants,
        int partySize
    ) {
        final var map = new HashMap<String, Object>();
        map.put("mode", modeName(language, anomaly.mode()));
        map.put("count", participants.size());
        map.put("party_size", partySize);
        map.put("participants", participantsText(language, participants));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, AnomalyResource::anomalyGathering),
            map
        );
    }

    public static String searching(
        Language language,
        List<EventParticipant> participants,
        int partySize,
        LaunchedEvent event
    ) {
        final var map = new HashMap<String, Object>();
        map.put("count", participants.size());
        map.put("party_size", partySize);
        map.put("end_date", TimeUtils.toString(event.endDate()));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, AnomalyResource::anomalySearching),
            map
        );
    }

    public static String challenge(
        Language language,
        List<EventParticipant> participants,
        int partySize
    ) {
        final var map = new HashMap<String, Object>();
        map.put("count", participants.size());
        map.put("party_size", partySize);
        map.put("participants", participantsText(language, participants));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, AnomalyResource::anomalyChallenge),
            map
        );
    }

    public static String safeCompleted(Language language, Money reward) {
        return rewardText(language, AnomalyResource::anomalySafeCompleted, reward, "");
    }

    public static String noMatch(Language language, Money reward) {
        return rewardText(language, AnomalyResource::anomalyNoMatch, reward, "");
    }

    public static String battleVictory(Language language, Money reward, String battleLink) {
        return rewardText(language, AnomalyResource::anomalyBattleVictory, reward, battleLink);
    }

    public static String battleDefeat(Language language, Money reward, String battleLink) {
        return rewardText(language, AnomalyResource::anomalyBattleDefeat, reward, battleLink);
    }

    public static String expired(Language language) {
        return resources.getOrDefault(language, AnomalyResource::anomalyExpired);
    }

    public static String startButton(Language language) {
        return resources.getOrDefault(language, AnomalyResource::startButton);
    }

    public static String safeModeButton(Language language) {
        return resources.getOrDefault(language, AnomalyResource::safeModeButton);
    }

    public static String dangerousModeButton(Language language) {
        return resources.getOrDefault(language, AnomalyResource::dangerousModeButton);
    }

    public static String joinButton(Language language) {
        return resources.getOrDefault(language, AnomalyResource::joinButton);
    }

    public static String readyButton(Language language) {
        return resources.getOrDefault(language, AnomalyResource::readyButton);
    }

    public static String backToOutpostButton(Language language) {
        return resources.getOrDefault(language, AnomalyResource::backToOutpostButton);
    }

    public static String successJoin(Language language) {
        return resources.getOrDefault(language, AnomalyResource::successJoin);
    }

    public static String successReadySafe(Language language) {
        return resources.getOrDefault(language, AnomalyResource::successReadySafe);
    }

    public static String successReadySearch(Language language) {
        return resources.getOrDefault(language, AnomalyResource::successReadySearch);
    }

    public static String successReadyBattle(Language language) {
        return resources.getOrDefault(language, AnomalyResource::successReadyBattle);
    }

    public static String error(Language language, AnomalyError error) {
        return switch (error) {
            case AnomalyError.NotRegistered _ ->
                resources.getOrDefault(language, AnomalyResource::errorNotRegistered);
            case AnomalyError.NoStormScanner _ ->
                resources.getOrDefault(language, AnomalyResource::errorNoStormScanner);
            case AnomalyError.AlreadyStartedToday _ ->
                resources.getOrDefault(language, AnomalyResource::errorAlreadyStartedToday);
            case AnomalyError.ActiveAnomalyExists _ ->
                resources.getOrDefault(language, AnomalyResource::errorActiveExists);
            case AnomalyError.NotGroupMember _ ->
                resources.getOrDefault(language, AnomalyResource::errorNotMember);
            case AnomalyError.NotOwner _ ->
                resources.getOrDefault(language, AnomalyResource::errorNotOwner);
            case AnomalyError.InvalidPhase _ ->
                resources.getOrDefault(language, AnomalyResource::errorInvalidPhase);
            case AnomalyError.PartyNotFull _ ->
                resources.getOrDefault(language, AnomalyResource::errorPartyNotFull);
            case AnomalyError.PartyEmpty _ ->
                resources.getOrDefault(language, AnomalyResource::errorPartyEmpty);
            case AnomalyError.RosterLocked _ ->
                resources.getOrDefault(language, AnomalyResource::errorRosterLocked);
            case AnomalyError.AlreadyJoined _ ->
                resources.getOrDefault(language, AnomalyResource::errorAlreadyJoined);
            case AnomalyError.PartyFull _ ->
                resources.getOrDefault(language, AnomalyResource::errorPartyFull);
            case AnomalyError.EventLocked _ ->
                resources.getOrDefault(language, AnomalyResource::errorEventLocked);
            case AnomalyError.FinalStatus _ ->
                resources.getOrDefault(language, AnomalyResource::errorFinal);
            case AnomalyError.EventNotFound _ ->
                resources.getOrDefault(language, AnomalyResource::errorNotFound);
        };
    }

    private static String modeName(Language language, Optional<AnomalyMode> mode) {
        return mode.map(value -> switch (value) {
            case SAFE -> resources.getOrDefault(language, AnomalyResource::modeSafe);
            case DANGEROUS -> resources.getOrDefault(language, AnomalyResource::modeDangerous);
        }).orElse("-");
    }

    private static String participantsText(Language language, List<EventParticipant> participants) {
        if (participants.isEmpty()) {
            return "-";
        }
        return participants.stream()
            .map(it -> StringNamedTemplate.format(
                resources.getOrDefault(language, AnomalyResource::participantLine),
                Map.of("name", LocaleUtils.personageNameWithBadge(it.personage()))
            ))
            .collect(Collectors.joining("\n"));
    }

    private static String rewardText(
        Language language,
        java.util.function.Function<AnomalyResource, String> template,
        Money reward,
        String battleLink
    ) {
        final var map = new HashMap<String, Object>();
        map.put("reward", reward.value());
        map.put("money_icon", Icons.MONEY);
        map.put("battle_link", battleLink == null ? "" : battleLink);
        return StringNamedTemplate.format(resources.getOrDefault(language, template), map);
    }
}
