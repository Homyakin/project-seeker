package ru.homyakin.seeker.telegram.command.group.duel;

import io.vavr.control.Either;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import ru.homyakin.seeker.game.duel.DuelService;
import ru.homyakin.seeker.game.duel.models.Duel;
import ru.homyakin.seeker.game.duel.models.DuelResult;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.group.stats.GroupStatsService;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.test_utils.DuelUtils;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.test_utils.TestRandom;
import ru.homyakin.seeker.test_utils.telegram.GroupUtils;
import ru.homyakin.seeker.test_utils.telegram.UserUtils;

import java.util.List;

public class AcceptDuelExecutorTest {
    private final DuelService duelService = Mockito.mock();
    private final TelegramSender telegramSender = Mockito.mock();
    private final GroupStatsService groupStatsService = Mockito.mock();
    private final UserService userService = Mockito.mock();
    private final AcceptDuelExecutor executor = new AcceptDuelExecutor(
        Mockito.mock(),
        duelService,
        telegramSender,
        groupStatsService,
        userService
    );
    private Group group;
    private User acceptor;
    private User initiator;
    private Personage acceptorPersonage;
    private Personage initiatorPersonage;
    private Duel duel;
    private AcceptDuel command;

    @BeforeEach
    public void init() {
        group = GroupUtils.randomGroup();
        acceptor = UserUtils.randomUser();
        initiator = UserUtils.randomUser();
        acceptorPersonage = PersonageUtils.withId(acceptor.personageId());
        initiatorPersonage = PersonageUtils.withId(initiator.personageId());
        duel = DuelUtils.withPersonages(initiatorPersonage.id(), acceptorPersonage.id());
        command = new AcceptDuel(
            RandomStringUtils.randomNumeric(10),
            group.id(),
            acceptor.id(),
            TestRandom.nextInt(),
            duel.id(),
            RandomStringUtils.randomAlphanumeric(20)
        );

        Mockito.when(duelService.getByIdForce(duel.id())).thenReturn(duel);
    }

    @Test
    public void Given_CorrectAcceptDuel_When_FinishDuel_And_InitiatorIsWinner_Then_SendDuelInitiatorWinnerResultToTelegram() {
        // given
        Mockito.when(duelService.finishDuel(duel, acceptorPersonage.id())).thenReturn(
            Either.right(
                new DuelResult(
                    initiatorPersonage.toBattlePersonage().toResult(),
                    acceptorPersonage.toBattlePersonage().toResult()
                )
            )
        );
        Mockito.when(userService.getByPersonageIdForce(initiatorPersonage.id())).thenReturn(initiator);
        final var finishDuelText = RandomStringUtils.randomAlphanumeric(10);
        final var winnerResultText = RandomStringUtils.randomAlphanumeric(10);
        final var loserResultText = RandomStringUtils.randomAlphanumeric(10);

        // when
        try (final var mock = Mockito.mockStatic(DuelLocalization.class)) {
            mock.when(() -> DuelLocalization
                    .finishedDuel(
                        group.language(),
                        TgPersonageMention.of(initiatorPersonage, initiator.id()),
                        TgPersonageMention.of(acceptorPersonage, acceptor.id())
                    )
                )
                .thenReturn(finishDuelText);
            mock.when(() -> DuelLocalization
                    .personageDuelResult(
                        group.language(),
                        initiatorPersonage.toBattlePersonage().toResult(),
                        true
                    )
                )
                .thenReturn(winnerResultText);
            mock.when(() -> DuelLocalization
                    .personageDuelResult(
                        group.language(),
                        acceptorPersonage.toBattlePersonage().toResult(),
                        false
                    )
                )
                .thenReturn(loserResultText);
            executor.processDuel(command, group, acceptor);
        }

        // then
        Mockito.verify(groupStatsService, Mockito.times(1)).increaseDuelsComplete(group.id(), initiatorPersonage.id(), acceptorPersonage.id());
        final var captor = ArgumentCaptor.forClass(EditMessageText.class);
        Mockito.verify(telegramSender).send(captor.capture());

        final var expected = EditMessageText.builder()
            .chatId(group.id().value())
            .messageId(command.messageId())
            .parseMode(ParseMode.HTML)
            .disableWebPagePreview(true)
            .entities(List.of())
            .text(finishDuelText + "\n\n" + winnerResultText + "\n" + loserResultText)
            .build();

        Assertions.assertEquals(expected, captor.getValue());
    }

    @Test
    public void Given_CorrectAcceptDuel_When_FinishDuel_And_AcceptorIsWinner_Then_SendDuelAcceptorWinnerResultToTelegram() {
        // given
        Mockito.when(duelService.finishDuel(duel, acceptorPersonage.id())).thenReturn(
            Either.right(
                new DuelResult(
                    acceptorPersonage.toBattlePersonage().toResult(),
                    initiatorPersonage.toBattlePersonage().toResult()
                )
            )
        );
        Mockito.when(userService.getByPersonageIdForce(initiatorPersonage.id())).thenReturn(initiator);
        final var finishDuelText = RandomStringUtils.randomAlphanumeric(10);
        final var winnerResultText = RandomStringUtils.randomAlphanumeric(10);
        final var loserResultText = RandomStringUtils.randomAlphanumeric(10);

        // when
        try (final var mock = Mockito.mockStatic(DuelLocalization.class)) {
            mock.when(() -> DuelLocalization
                    .finishedDuel(
                        group.language(),
                        TgPersonageMention.of(acceptorPersonage, acceptor.id()),
                        TgPersonageMention.of(initiatorPersonage, initiator.id())
                    )
                )
                .thenReturn(finishDuelText);
            mock.when(() -> DuelLocalization
                    .personageDuelResult(
                        group.language(),
                        initiatorPersonage.toBattlePersonage().toResult(),
                        false
                    )
                )
                .thenReturn(loserResultText);
            mock.when(() -> DuelLocalization
                    .personageDuelResult(
                        group.language(),
                        acceptorPersonage.toBattlePersonage().toResult(),
                        true
                    )
                )
                .thenReturn(winnerResultText);
            executor.processDuel(command, group, acceptor);
        }

        // then
        Mockito.verify(groupStatsService, Mockito.times(1)).increaseDuelsComplete(group.id(), acceptorPersonage.id(), initiatorPersonage.id());
        final var captor = ArgumentCaptor.forClass(EditMessageText.class);
        Mockito.verify(telegramSender).send(captor.capture());
        System.out.println(captor.getValue());
        final var expected = EditMessageText.builder()
            .chatId(group.id().value())
            .messageId(command.messageId())
            .parseMode(ParseMode.HTML)
            .disableWebPagePreview(true)
            .entities(List.of())
            .text(finishDuelText + "\n\n" + winnerResultText + "\n" + loserResultText)
            .build();

        Assertions.assertEquals(expected, captor.getValue());
    }
}
