package ru.homyakin.seeker.game.event.raid.generator;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.game.event.service.GroupEventService;

import java.util.List;

@Component
public class RaidGenerator {
    private final GroupEventService groupEventService;

    public RaidGenerator(GroupEventService groupEventService) {
        this.groupEventService = groupEventService;
    }

    public List<BattlePersonage> generate(Raid raid, LaunchedEvent event, List<BattlePersonage> personages) {

        return raid.template().generate(personages, calcPowerPercent(event));
    }

    private double calcPowerPercent(LaunchedEvent event) {
        final var groups = groupEventService.getByLaunchedEventId(event.id());
        // Тут костыль. Если одна группа постоянно проигрывает, то мы хотим увеличить вероятность выигрыша
        // Функционал событий/рейдов на несколько групп пока (08.07.2024) не реализован, поэтому пока возвращаем полную силу рейда
        if (groups.size() != 1) {
            return 1.0;
        }

        final var group = groups.getFirst();

        final var failedRaidsCount = groupEventService.countFailedEventsFromLastSuccessInGroup(group.groupId());

        return 1.0 - 0.05 * failedRaidsCount;
    }

}
