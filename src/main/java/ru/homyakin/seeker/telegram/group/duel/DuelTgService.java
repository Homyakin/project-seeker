package ru.homyakin.seeker.telegram.group.duel;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import ru.homyakin.seeker.game.duel.DuelService;
import ru.homyakin.seeker.game.duel.models.Duel;
import ru.homyakin.seeker.game.duel.models.DuelError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.infrastructure.PersonageMention;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.models.TelegramError;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class DuelTgService {
    private static final Logger logger = LoggerFactory.getLogger(DuelTgService.class);
    private final DuelTgDao duelTgDao;
    private final DuelService duelService;
    private final TelegramSender telegramSender;
    private final PersonageService personageService;
    private final GroupService groupService;
    private final UserService userService;

    public DuelTgService(
        DuelTgDao duelTgDao,
        DuelService duelService,
        TelegramSender telegramSender,
        PersonageService personageService,
        GroupService groupService,
        UserService userService
    ) {
        this.duelTgDao = duelTgDao;
        this.duelService = duelService;
        this.telegramSender = telegramSender;
        this.personageService = personageService;
        this.groupService = groupService;
        this.userService = userService;
    }

    public Either<DuelError, DuelTg> createDuel(
        User initiatingUser,
        User acceptingUser,
        Group group
    ) {
        final var initiatingPersonage = personageService.getByIdForce(initiatingUser.personageId());
        final var acceptingPersonage = personageService.getByIdForce(acceptingUser.personageId());
        return duelService.createDuel(initiatingPersonage, acceptingPersonage)
            .flatMap(duel -> sendMessage(
                    TgPersonageMention.of(initiatingPersonage, initiatingUser.id()),
                    TgPersonageMention.of(acceptingPersonage, acceptingUser.id()),
                    group,
                    duel
                ).map(
                    message -> linkDuelToMessage(duel.id(), group.id(), message.getMessageId())
                ).mapLeft(
                    error -> {
                        logger.warn("Can't send message to telegram: " + error.toString());
                        return new DuelError.InternalError();
                    }
                )
            );
    }

    @Scheduled(cron = "0 * * * * *")
    public void expireOldDuels() {
        logger.debug("Expiring duels");
        duelTgDao.findNotFinalWithLessExpireDateTime(TimeUtils.moscowTime()).forEach(
            duelTg -> {
                logger.info("Duel " + duelTg.duelId() + " was expired");
                duelService.expireDuel(duelTg.duelId());
                final var duel = duelService.getByIdForce(duelTg.duelId());
                final var group = groupService.getOrCreate(duelTg.groupTgId());
                final var acceptor = personageService.getByIdForce(duel.acceptingPersonageId());
                final var user = userService.getByPersonageIdForce(acceptor.id());
                telegramSender.send(
                    EditMessageTextBuilder.builder()
                        .chatId(group.id())
                        .messageId(duelTg.messageId())
                        .text(DuelLocalization.expiredDuel(group.language(), TgPersonageMention.of(acceptor, user.id())))
                        .build()
                );
            }
        );
    }

    private Either<TelegramError, Message> sendMessage(
        PersonageMention initiatorMention,
        PersonageMention acceptorMention,
        Group group,
        Duel duel
    ) {
        return telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(group.id())
                .text(DuelLocalization.initDuel(group.language(), initiatorMention, acceptorMention))
                .keyboard(InlineKeyboards.duelKeyboard(group.language(), duel.id()))
                .build()
        );
    }

    private DuelTg linkDuelToMessage(long duelId, GroupId groupId, int messageId) {
        return duelTgDao.insert(
            new DuelTg(
                duelId,
                groupId,
                messageId
            )
        );
    }
}
