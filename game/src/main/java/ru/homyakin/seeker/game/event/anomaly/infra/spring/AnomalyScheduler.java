package ru.homyakin.seeker.game.event.anomaly.infra.spring;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.anomaly.action.AnomalyMatchmaker;

@Component
public class AnomalyScheduler {
    private final AnomalyMatchmaker anomalyMatchmaker;

    public AnomalyScheduler(AnomalyMatchmaker anomalyMatchmaker) {
        this.anomalyMatchmaker = anomalyMatchmaker;
    }

    @Scheduled(initialDelay = 10 * 1000, fixedRate = 30 * 1000)
    public void matchAnomalies() {
        anomalyMatchmaker.matchSearchingExpeditions();
    }
}
