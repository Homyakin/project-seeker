package ru.homyakin.seeker.telegram.world_raid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.world_raid.action.NotifyAboutWorldRaidResearchEndCommand;
import ru.homyakin.seeker.game.event.world_raid.action.SendWorldRaidBattleCommand;
import ru.homyakin.seeker.game.event.world_raid.action.SendWorldRaidBattleResultCommand;
import ru.homyakin.seeker.game.event.world_raid.action.SendWorldRaidBattleToGroupCommand;
import ru.homyakin.seeker.game.event.world_raid.action.SendWorldRaidBattleUpdateCommand;
import ru.homyakin.seeker.game.event.world_raid.entity.ActiveWorldRaid;
import ru.homyakin.seeker.game.event.world_raid.entity.battle.GroupWorldRaidBattleResult;
import ru.homyakin.seeker.game.group.action.GetGroup;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.top.TopService;
import ru.homyakin.seeker.locale.world_raid.WorldRaidLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

import java.util.List;

@Component
public class TelegramWorldRaidService implements NotifyAboutWorldRaidResearchEndCommand,
    SendWorldRaidBattleCommand, SendWorldRaidBattleUpdateCommand, SendWorldRaidBattleToGroupCommand,
    SendWorldRaidBattleResultCommand {

    private static final Logger logger = LoggerFactory.getLogger(TelegramWorldRaidService.class);
    private final TelegramWorldRaidDao dao;
    private final TelegramWorldRaidConfig config;
    private final TopService topService;
    private final GroupTgService groupTgService;
    private final TelegramSender telegramSender;
    private final GetGroup getGroup;

    public TelegramWorldRaidService(
        TelegramWorldRaidDao dao,
        TelegramWorldRaidConfig config,
        TopService topService,
        GroupTgService groupTgService,
        TelegramSender telegramSender,
        GetGroup getGroup
    ) {
        this.dao = dao;
        this.config = config;
        this.topService = topService;
        this.groupTgService = groupTgService;
        this.telegramSender = telegramSender;
        this.getGroup = getGroup;
    }

    @Override
    public void notifyAboutResearchEnd() {
        for (final var entry : config.channels().entrySet()) {
            telegramSender.send(
                SendMessageBuilder.builder()
                    .chatId(entry.getValue())
                    .text(
                        WorldRaidLocalization.worldRaidResearchEnd(
                            entry.getKey(),
                            topService.getTopWorldRaidResearch()
                        )
                    )
                    .build()
            );
        }
    }

    @Override
    public void sendBattle(
        ActiveWorldRaid raid,
        LaunchedEvent event,
        int requiredEnergy
    ) {
        for (final var entry : config.channels().entrySet()) {
            final var message = telegramSender.send(
                SendMessageBuilder.builder()
                    .chatId(entry.getValue())
                    .text(WorldRaidLocalization.worldRaidBattle(entry.getKey(), raid, event))
                    .keyboard(InlineKeyboards.joinWorldRaidKeyboard(entry.getKey(), requiredEnergy))
                    .build()
            );
            if (message.isRight()) {
                dao.save(
                    new TelegramWorldRaid(
                        raid.id(),
                        message.get().getChatId(),
                        entry.getKey(),
                        message.get().getMessageId()
                    )
                );
            } else {
                logger.error("Unable send world raid battle message. {}", message.getLeft());
            }
        }
    }

    @Override
    public void sendUpdate(
        ActiveWorldRaid raid,
        LaunchedEvent event,
        int participantsCount,
        List<Group> groups,
        int requiredEnergy
    ) {
        final var telegramWorldRaids = dao.getByWorldRaidId(raid.id());
        for (final var telegramWorldRaid : telegramWorldRaids) {
            telegramSender.send(
                EditMessageTextBuilder.builder()
                    .chatId(telegramWorldRaid.channelId())
                    .messageId(telegramWorldRaid.messageId())
                    .text(WorldRaidLocalization.worldRaidBattle(
                        telegramWorldRaid.language(),
                        raid,
                        event,
                        groups,
                        participantsCount
                    ))
                    .keyboard(
                        InlineKeyboards.joinWorldRaidKeyboard(telegramWorldRaid.language(), requiredEnergy)
                    )
                    .build()
            );
        }
    }

    @Override
    public void sendBattleToGroup(GroupId groupId, LaunchedEvent event) {
        if (!getGroup.forceGet(groupId).isActive()) {
            return;
        }
        final var groupTg = groupTgService.forceGet(groupId);
        final var language = groupTg.language();
        try {
            // На всякий случай защита от лимитов телеги
            Thread.sleep(300);
        } catch (InterruptedException _) {
        }
        telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(groupTg.id())
                .text(WorldRaidLocalization.groupNotification(language, config.getOrDefaultChannel(language), event))
                .build()
        );
    }

    @Override
    public void sendBattleResult(
        EventResult.WorldRaidBattleResult result,
        ActiveWorldRaid raid
    ) {
        final var telegramWorldRaids = dao.getByWorldRaidId(raid.id());
        for (final var telegramWorldRaid : telegramWorldRaids) {
            telegramSender.send(
                EditMessageTextBuilder.builder()
                    .chatId(telegramWorldRaid.channelId())
                    .messageId(telegramWorldRaid.messageId())
                    .text(WorldRaidLocalization.endedWorldRaidBattle(
                        telegramWorldRaid.language(),
                        raid,
                        result.groupResults().stream().map(GroupWorldRaidBattleResult::group).toList(),
                        result.personageResults().size()
                    ))
                    .build()
            );
            telegramSender.send(
                SendMessageBuilder.builder()
                    .chatId(telegramWorldRaid.channelId())
                    .text(WorldRaidLocalization.raidBattleResult(telegramWorldRaid.language(), result))
                    .replyMessageId(telegramWorldRaid.messageId())
                    .build()
            );
        }
    }
}
