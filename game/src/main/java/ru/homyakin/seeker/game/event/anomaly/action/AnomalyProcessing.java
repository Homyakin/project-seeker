package ru.homyakin.seeker.game.event.anomaly.action;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.models.EventResult;

@Service
public class AnomalyProcessing {
    private final AnomalyService anomalyService;

    public AnomalyProcessing(AnomalyService anomalyService) {
        this.anomalyService = anomalyService;
    }

    public EventResult process(LaunchedEvent launchedEvent) {
        return anomalyService.processExpired(launchedEvent);
    }
}
