package ru.homyakin.seeker.telegram.world_raid;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.homyakin.seeker.locale.Language;

import java.util.Map;

@ConfigurationProperties("homyakin.seeker.telegram.world-raid")
public class TelegramWorldRaidConfig {
    private Map<Language, String> channels;

    public void setChannels(Map<Language, String> channels) {
        assert channels.containsKey(Language.DEFAULT);
        this.channels = channels;
    }

    public Map<Language, String> channels() {
        return channels;
    }

    public String getOrDefaultChannel(Language language) {
        return channels.getOrDefault(language, channels.get(Language.DEFAULT));
    }
}
