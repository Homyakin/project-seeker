package ru.homyakin.seeker.game.group.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "homyakin.seeker.group.members-list")
public class GroupMembersListConfig {
    private int pageSize = 20;

    public int pageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}
