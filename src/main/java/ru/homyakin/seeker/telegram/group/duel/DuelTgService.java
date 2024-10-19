package ru.homyakin.seeker.telegram.group.duel;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.game.duel.DuelService;
import ru.homyakin.seeker.game.duel.models.CreateDuelError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
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
    private final GroupTgService groupTgService;
    private final UserService userService;

    public DuelTgService(
        DuelTgDao duelTgDao,
        DuelService duelService,
        TelegramSender telegramSender,
        PersonageService personageService,
        GroupTgService groupTgService,
        UserService userService
    ) {
        this.duelTgDao = duelTgDao;
        this.duelService = duelService;
        this.telegramSender = telegramSender;
        this.personageService = personageService;
        this.groupTgService = groupTgService;
        this.userService = userService;
    }

    public Either<CreateDuelError, CreateDuelTgResult> createDuel(
        User initiatingUser,
        User acceptingUser,
        GroupTg group
    ) {
        final var initiatingPersonage = personageService.getByIdForce(initiatingUser.personageId());
        final var acceptingPersonage = personageService.getByIdForce(acceptingUser.personageId());
        return duelService.createDuel(initiatingPersonage, acceptingPersonage)
            .map(result ->
                new CreateDuelTgResult(
                    result.duelId(),
                    TgPersonageMention.of(result.initiatingPersonage(), initiatingUser.id()),
                    TgPersonageMention.of(result.acceptingPersonage(), acceptingUser.id()),
                    result.cost()
                )
            )
            .flatMap(result -> sendMessage(result, group)
                .peek(message -> linkDuelToMessage(result.duelId(), group.id(), message.getMessageId()))
                .map(_ -> result)
                .mapLeft(
                    error -> {
                        duelService.expireDuel(result.duelId());
                        logger.warn("Can't send message to telegram: " + error.toString());
                        return new CreateDuelError.InternalError();
                    }
                )
            );
    }

    @Scheduled(cron = "0 * * * * *")
    public void expireOldDuels() {
        logger.debug("Expiring duels");
        duelTgDao.findNotFinalWithLessExpireDateTime(TimeUtils.moscowTime()).forEach(
            duelTg -> {
                logger.info("Duel " + duelTg.duelId() + " expired");
                duelService.expireDuel(duelTg.duelId())
                    .peek(success -> {
                        final var duel = duelService.getByIdForce(duelTg.duelId());
                        final var group = groupTgService.getOrCreate(duelTg.groupTgId());
                        final var acceptor = personageService.getByIdForce(duel.acceptingPersonageId());
                        final var user = userService.getByPersonageIdForce(acceptor.id());
                        telegramSender.send(
                            EditMessageTextBuilder.builder()
                                .chatId(group.id())
                                .messageId(duelTg.messageId())
                                .text(DuelLocalization.expiredDuel(group.language(), TgPersonageMention.of(acceptor, user.id())))
                                .build()
                        );
                    });
            }
        );
    }

    private Either<TelegramError, Message> sendMessage(CreateDuelTgResult result, GroupTg group) {
        return telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(group.id())
                .text(DuelLocalization.initDuel(group.language(), result))
                .keyboard(InlineKeyboards.duelKeyboard(group.language(), result.duelId()))
                .build()
        );
    }

    private void linkDuelToMessage(long duelId, GroupTgId groupId, int messageId) {
        duelTgDao.save(
            new DuelTg(
                duelId,
                groupId,
                messageId
            )
        );
    }
}
