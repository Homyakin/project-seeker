package ru.homyakin.seeker.telegram.command.group.duel;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.duel.DuelService;
import ru.homyakin.seeker.game.duel.models.DuelError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.models.MentionInfo;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class StartDuelExecutor extends CommandExecutor<StartDuel> {
    private static final Logger logger = LoggerFactory.getLogger(StartDuelExecutor.class);
    private final GroupUserService groupUserService;
    private final UserService userService;
    private final PersonageService personageService;
    private final DuelService duelService;
    private final TelegramSender telegramSender;

    public StartDuelExecutor(
        GroupUserService groupUserService,
        UserService userService,
        PersonageService personageService,
        DuelService duelService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.userService = userService;
        this.personageService = personageService;
        this.duelService = duelService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(StartDuel command) {
        final var groupUserPair = groupUserService.getAndActivateOrCreate(
            command.groupId(), command.userId()
        );
        final var group = groupUserPair.first();
        final var initiatingUser = groupUserPair.second();
        final var validationResult = validateCommand(command, group.language());
        if (validationResult.isLeft()) {
            telegramSender.send(
                SendMessageBuilder.builder().chatId(command.groupId()).text(validationResult.getLeft()).build()
            );
            return;
        }

        final var userResult = userService.tryGetOrCreateByMention(validationResult.get(), group.id());
        if (userResult.isEmpty()) {
            logger.warn("Unknown mention group={}, mention={}", group.id(), validationResult.get());
            telegramSender.send(
                SendMessageBuilder
                    .builder()
                    .chatId(command.groupId())
                    .text(DuelLocalization.duelWithUnknownUser(group.language()))
                    .build()
            );
            return;
        } else if (userResult.get().id() == initiatingUser.id()) {
            telegramSender.send(
                SendMessageBuilder
                    .builder()
                    .chatId(command.groupId())
                    .text(DuelLocalization.duelWithYourself(group.language()))
                    .build()
            );
            return;
        }
        final var acceptingUser = userResult.get();
        final var initiatingPersonage = personageService.getByIdForce(initiatingUser.personageId());
        final var acceptingPersonage = personageService.getByIdForce(acceptingUser.personageId());

        final var duelResult = duelService.createDuel(initiatingPersonage, acceptingPersonage, group.id());
        if (duelResult.isLeft()) {
            final var error = duelResult.getLeft();
            final var message = switch (error) {
                case DuelError.PersonageAlreadyHasDuel ignored ->
                    DuelLocalization.personageAlreadyStartDuel(group.language());
                case DuelError.InitiatingPersonageNotEnoughMoney notEnoughMoney ->
                    DuelLocalization.duelWithInitiatorNotEnoughMoney(group.language(), notEnoughMoney.money());
            };
            telegramSender.send(
                SendMessageBuilder.builder().chatId(group.id()).text(message).build()
            );
            return;
        }
        final var telegramResult = telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(group.id())
                .text(
                    DuelLocalization.initDuel(
                        group.language(),
                        TgPersonageMention.of(initiatingPersonage, initiatingUser.id()),
                        TgPersonageMention.of(acceptingPersonage, acceptingUser.id())
                    )
                )
                .keyboard(InlineKeyboards.duelKeyboard(group.language(), duelResult.get().id()))
                .build()
        );
        if (telegramResult.isLeft()) {
            logger.error("Can't send duel to group");
            return;
        }
        duelService.addMessageIdToDuel(duelResult.get().id(), telegramResult.get().getMessageId());
    }

    private Either<String, MentionInfo> validateCommand(StartDuel command, Language language) {
        if (command.mentionInfo().isEmpty()) {
            return Either.left(DuelLocalization.duelMustContainsMention(language));
        }
        return switch (command.mentionInfo().get().userType()) {
            case USER -> Either.right(command.mentionInfo().get());
            case DIFFERENT_BOT -> Either.left(DuelLocalization.duelWithDifferentBot(language));
            case THIS_BOT -> Either.left(DuelLocalization.duelWithThisBot(language));
        };
    }

}
