package ru.homyakin.seeker.telegram.user.state;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("homyakin.seeker.feedback")
public record FeedbackConfig(
    long adminGroup
) {
}
