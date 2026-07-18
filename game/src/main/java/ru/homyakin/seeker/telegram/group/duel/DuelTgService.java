package ru.homyakin.seeker.telegram.group.duel;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.homyakin.seeker.game.duel.DuelService;
import ru.homyakin.seeker.game.duel.models.CreateDuelError;
import ru.homyakin.seeker.game.event.service.GroupEventService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.models.TelegramError;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Service
public class DuelTgService {
    private static final Logger logger = LoggerFactory.getLogger(DuelTgService.class);
    private final DuelService duelService;
    private final TelegramSender telegramSender;
    private final PersonageService personageService;
    private final GroupEventService groupEventService;

    public DuelTgService(
        DuelService duelService,
        TelegramSender telegramSender,
        PersonageService personageService,
        GroupEventService groupEventService
    ) {
        this.duelService = duelService;
        this.telegramSender = telegramSender;
        this.personageService = personageService;
        this.groupEventService = groupEventService;
    }

    public Either<CreateDuelError, CreateDuelTgResult> createDuel(
        User initiatingUser,
        User acceptingUser,
        GroupTg group
    ) {
        final var initiatingPersonage = personageService.getByIdForce(initiatingUser.personageId());
        final var acceptingPersonage = personageService.getByIdForce(acceptingUser.personageId());
        return duelService.createDuel(initiatingPersonage, acceptingPersonage, group.domainGroupId())
            .map(result ->
                new CreateDuelTgResult(
                    result.duelId(),
                    TgPersonageMention.of(result.initiatingPersonage(), initiatingUser.id()),
                    TgPersonageMention.of(result.acceptingPersonage(), acceptingUser.id()),
                    result.cost()
                )
            )
            .flatMap(result -> sendMessage(result, group)
                .peek(message -> groupEventService.createGroupEvent(
                    result.duelId(),
                    group,
                    message.getMessageId()
                ))
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

    private Either<TelegramError, Message> sendMessage(CreateDuelTgResult result, GroupTg group) {
        return telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(group.id())
                .text(DuelLocalization.initDuel(group.language(), result))
                .keyboard(InlineKeyboards.duelKeyboard(group.language(), result.duelId()))
                .build()
        );
    }
}
