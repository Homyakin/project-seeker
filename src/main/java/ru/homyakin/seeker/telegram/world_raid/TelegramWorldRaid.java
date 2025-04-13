package ru.homyakin.seeker.telegram.world_raid;

import ru.homyakin.seeker.locale.Language;

public record TelegramWorldRaid(
    long worldRaidId,
    long channelId,
    Language language,
    int messageId
) {
}
