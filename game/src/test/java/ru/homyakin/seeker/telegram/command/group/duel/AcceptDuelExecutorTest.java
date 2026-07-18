package ru.homyakin.seeker.telegram.command.group.duel;

import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import ru.homyakin.seeker.game.battle.BattleVisualizerConfig;
import ru.homyakin.seeker.game.duel.DuelService;
import ru.homyakin.seeker.game.duel.models.Duel;
import ru.homyakin.seeker.game.duel.models.DuelPersonageResult;
import ru.homyakin.seeker.game.duel.models.DuelResult;
import ru.homyakin.seeker.game.battle.BattlePersonageStats;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.locale.battle.BattleLocalization;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.game.stats.action.GroupStatsService;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
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
    private final BattleVisualizerConfig battleVisualizerConfig = new BattleVisualizerConfig("https://example.com/?id=");
    private final AcceptDuelExecutor executor = new AcceptDuelExecutor(
        Mockito.mock(),
        duelService,
        telegramSender,
        groupStatsService,
        userService,
        battleVisualizerConfig
    );
    private GroupTg group;
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
            TestRandom.randomNumeric(10),
            group.id(),
            acceptor.id(),
            TestRandom.nextInt(),
            duel.id(),
            TestRandom.randomAlphanumeric(20)
        );

        Mockito.when(duelService.getByIdForce(duel.id())).thenReturn(duel);
    }

    @Test
    public void Given_CorrectAcceptDuel_When_FinishDuel_And_InitiatorIsWinner_Then_SendDuelInitiatorWinnerResultToTelegram() {
        // given
        final var duelResult = new DuelResult(
            duelPersonageResult(initiatorPersonage),
            duelPersonageResult(acceptorPersonage),
            7
        );
        Mockito.when(duelService.finishDuel(duel, acceptorPersonage.id())).thenReturn(
            Either.right(duelResult)
        );
        Mockito.when(userService.getByPersonageIdForce(initiatorPersonage.id())).thenReturn(initiator);
        final var finishDuelResultText = TestRandom.randomAlphanumeric(10);
        final var visualizerButton = TestRandom.randomAlphanumeric(10);

        // when / then
        try (
            final var duelLocale = Mockito.mockStatic(DuelLocalization.class);
            final var battleLocale = Mockito.mockStatic(BattleLocalization.class)
        ) {
            duelLocale.when(() -> DuelLocalization
                    .finishedDuelResult(
                        group.language(),
                        TgPersonageMention.of(initiatorPersonage, initiator.id()),
                        TgPersonageMention.of(acceptorPersonage, acceptor.id()),
                        duelResult
                    )
                )
                .thenReturn(finishDuelResultText);
            battleLocale.when(() -> BattleLocalization.battleVisualizerButton(group.language()))
                .thenReturn(visualizerButton);
            executor.processDuel(command, group, acceptor);

            Mockito.verify(groupStatsService, Mockito.times(1)).increaseDuelsComplete(
                group.domainGroupId(), initiatorPersonage.id(), acceptorPersonage.id()
            );
            final var captor = ArgumentCaptor.forClass(EditMessageText.class);
            Mockito.verify(telegramSender).send(captor.capture());

            final var expected = EditMessageText.builder()
                .chatId(group.id().value())
                .messageId(command.messageId())
                .parseMode(ParseMode.HTML)
                .disableWebPagePreview(true)
                .entities(List.of())
                .text(finishDuelResultText)
                .replyMarkup(InlineKeyboards.battleVisualizerKeyboard(
                    group.language(),
                    battleVisualizerConfig.battleUrl(duel.id())
                ))
                .build();

            Assertions.assertEquals(expected, captor.getValue());
        }
    }

    @Test
    public void Given_CorrectAcceptDuel_When_FinishDuel_And_AcceptorIsWinner_Then_SendDuelAcceptorWinnerResultToTelegram() {
        // given
        final var duelResult = new DuelResult(
            duelPersonageResult(acceptorPersonage),
            duelPersonageResult(initiatorPersonage),
            5
        );
        Mockito.when(duelService.finishDuel(duel, acceptorPersonage.id())).thenReturn(
            Either.right(duelResult)
        );
        Mockito.when(userService.getByPersonageIdForce(initiatorPersonage.id())).thenReturn(initiator);
        final var finishDuelResultText = TestRandom.randomAlphanumeric(10);
        final var visualizerButton = TestRandom.randomAlphanumeric(10);

        // when / then
        try (
            final var duelLocale = Mockito.mockStatic(DuelLocalization.class);
            final var battleLocale = Mockito.mockStatic(BattleLocalization.class)
        ) {
            duelLocale.when(() -> DuelLocalization
                    .finishedDuelResult(
                        group.language(),
                        TgPersonageMention.of(acceptorPersonage, acceptor.id()),
                        TgPersonageMention.of(initiatorPersonage, initiator.id()),
                        duelResult
                    )
                )
                .thenReturn(finishDuelResultText);
            battleLocale.when(() -> BattleLocalization.battleVisualizerButton(group.language()))
                .thenReturn(visualizerButton);
            executor.processDuel(command, group, acceptor);

            Mockito.verify(groupStatsService, Mockito.times(1)).increaseDuelsComplete(
                group.domainGroupId(), acceptorPersonage.id(), initiatorPersonage.id()
            );
            final var captor = ArgumentCaptor.forClass(EditMessageText.class);
            Mockito.verify(telegramSender).send(captor.capture());
            final var expected = EditMessageText.builder()
                .chatId(group.id().value())
                .messageId(command.messageId())
                .parseMode(ParseMode.HTML)
                .disableWebPagePreview(true)
                .entities(List.of())
                .text(finishDuelResultText)
                .replyMarkup(InlineKeyboards.battleVisualizerKeyboard(
                    group.language(),
                    battleVisualizerConfig.battleUrl(duel.id())
                ))
                .build();

            Assertions.assertEquals(expected, captor.getValue());
        }
    }

    private static DuelPersonageResult duelPersonageResult(Personage personage) {
        return new DuelPersonageResult(
            personage,
            new BattlePersonageStats(100, 50, 0, 0, 10, 1, 5, 0, 0, 0, 0, 0, 0, 3, 0, 0)
        );
    }
}
