package ru.homyakin.seeker.game.event.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.TwoPersonageTeamsBattle;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;

@Service
public class BossProcessing {
    private final PersonageService personageService;
    private final TwoPersonageTeamsBattle twoPersonageTeamsBattle;

    public BossProcessing(
        PersonageService personageService,
        TwoPersonageTeamsBattle twoPersonageTeamsBattle
    ) {
        this.personageService = personageService;
        this.twoPersonageTeamsBattle = twoPersonageTeamsBattle;
    }

    public EventResult process(Event event, List<Personage> participants) {
        final var bossPersonage = personageService.getByBossEvent(event.id())
            .orElseThrow(() -> new IllegalStateException("Boss event must contain personage " + event.id()));

        final var personages = new ArrayList<BattlePersonage>(participants.size());
        for (final var participant: participants) {
            personages.add(participant.toBattlePersonage());
        }

        final var result = twoPersonageTeamsBattle.battle(
            List.of(bossPersonage.toBattlePersonage()),
            personages
        );
        // TODO система получения опыта
        // personages.sort(Comparator.comparingInt(BattlePersonage::damageDealtAndTaken));

        if (result instanceof TwoPersonageTeamsBattle.Result.FirstTeamWin) {
            return new EventResult.Failure();
        } else {
            for (final var participant: participants) {
                personageService.addExperience(participant, bossPersonage.level());
            }
            return new EventResult.Success();
        }
    }
}
