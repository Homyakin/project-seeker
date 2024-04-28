package ru.homyakin.seeker.game.personage;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("homyakin.seeker.personage")
public class PersonageConfig {
    private Duration energyFullRecovery;
    private int raidEnergyCost;

    public Duration energyFullRecovery() {
        return energyFullRecovery;
    }

    public int raidEnergyCost() {
        return raidEnergyCost;
    }

    public void setEnergyFullRecovery(Duration energyFullRecovery) {
        this.energyFullRecovery = energyFullRecovery;
    }

    public void setRaidEnergyCost(int raidEnergyCost) {
        this.raidEnergyCost = raidEnergyCost;
    }
}
