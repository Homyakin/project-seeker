package ru.homyakin.seeker.telegram.group;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "homyakin.seeker.everyday-spin")
public class EverydaySpinConfig {
    private Integer minimumUsers;

    public Integer minimumUsers() {
        return minimumUsers;
    }

    public void setMinimumUsers(Integer minimumUsers) {
        this.minimumUsers = minimumUsers;
    }
}
