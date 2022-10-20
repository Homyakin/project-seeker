package ru.homyakin.seeker.game.event.service;

import java.util.List;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.battle.TwoPersonageTeamsBattle;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;

@Service
public class BossProcessing {
    private final PersonageService personageService;

    public BossProcessing(PersonageService personageService) {
        this.personageService = personageService;
    }

    public EventResult process(Event event, List<Personage> participants) {
        final var bossPersonage = personageService.getByBossEvent(event.id())
            .orElseThrow(() -> new IllegalStateException("Boss event must contain personage " + event.id()));

        final var result = TwoPersonageTeamsBattle.battle(List.of(bossPersonage), participants);
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
