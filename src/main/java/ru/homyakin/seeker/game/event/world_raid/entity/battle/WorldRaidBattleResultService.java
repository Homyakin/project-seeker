package ru.homyakin.seeker.game.event.world_raid.entity.battle;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.v3.GroupBattleResult;
import ru.homyakin.seeker.game.battle.v3.TeamResult;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidBattleInfo;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.game.group.action.GroupBattleResultService;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;

import java.util.stream.Collectors;

@Component
public class WorldRaidBattleResultService {
    private final GetGroup getGroup;
    private final WorldRaidItemGenerator worldRaidItemGenerator;
    private final PersonageService personageService;
    private final GroupBattleResultService groupBattleResultService;

    public WorldRaidBattleResultService(
        GetGroup getGroup,
        WorldRaidItemGenerator worldRaidItemGenerator,
        PersonageService personageService,
        GroupBattleResultService groupBattleResultService
    ) {
        this.getGroup = getGroup;
        this.worldRaidItemGenerator = worldRaidItemGenerator;
        this.personageService = personageService;
        this.groupBattleResultService = groupBattleResultService;
    }

    /**
     * Считает результаты мирового рейда, а также проставляет награды и результаты
     */
    public EventResult.WorldRaidBattleResult processResult(
        Money fund,
        TeamResult result,
        boolean isWin,
        LaunchedEvent launchedEvent,
        WorldRaidBattleInfo remainInfo
    ) {
        int moneyForPersonages = fund.value() / 2;
        int moneyForGroup = fund.value() - moneyForPersonages;

        final var personageTotalImpact = result.personageResults().stream()
            .mapToLong(it -> it.stats().damageDealtAndTaken())
            .sum();
        final var groupTotalImpact = result.groupResults().stream()
            .mapToLong(it -> it.stats().damageDealtAndTaken())
            .sum();

        final var personageResults = result.personageResults().stream()
            .map(it -> new PersonageWorldRaidBattleResult(
                it.personage(),
                it.stats(),
                Money.from((int) (moneyForPersonages * it.stats().damageDealtAndTaken() / personageTotalImpact)),
                worldRaidItemGenerator.generate(it.personage(), isWin)
            ))
            .toList();

        final var tagToGroup = getGroup.getByTags(
                result.groupResults().stream()
                    .map(GroupBattleResult::tag)
                    .toList()
            ).stream()
            .filter(it -> it.tag().isPresent())
            .collect(Collectors.toMap(it -> it.tag().get(), it -> it));
        final var groupResults = result.groupResults().stream()
            .map(it -> new GroupWorldRaidBattleResult(
                tagToGroup.get(it.tag()),
                it.stats(),
                Money.from((int) (moneyForGroup * it.stats().damageDealtAndTaken() / groupTotalImpact))
            ))
            .toList();

        personageService.saveWorldRaidResults(personageResults, launchedEvent);
        groupBattleResultService.saveWorldRaidResults(groupResults, launchedEvent);

        return new EventResult.WorldRaidBattleResult(isWin, groupResults, personageResults, remainInfo);
    }
}
