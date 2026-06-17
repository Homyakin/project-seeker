package ru.homyakin.seeker.telegram.command.group.duel;

import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import ru.homyakin.seeker.game.duel.DuelService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.test_utils.DuelUtils;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.test_utils.TestRandom;
import ru.homyakin.seeker.test_utils.telegram.GroupUtils;
import ru.homyakin.seeker.test_utils.telegram.UserUtils;
import ru.homyakin.seeker.utils.models.Success;

import java.util.List;

public class DeclineDuelExecutorTest {
    private final DuelService duelService = Mockito.mock();
    private final TelegramSender telegramSender = Mockito.mock();
    private final PersonageService personageService = Mockito.mock();
    private final UserService userService = Mockito.mock();
    private final DeclineDuelExecutor executor = new DeclineDuelExecutor(
        Mockito.mock(GroupUserService.class),
        duelService,
        telegramSender,
        personageService,
        userService
    );

    @Test
    public void Given_CorrectDeclineDuel_When_FinishDuel_And_InitiatorIsWinner_Then_SendDuelInitiatorWinnerResultToTelegram() {
        // given
        final var group = GroupUtils.randomGroup();
        final var acceptor = UserUtils.randomUser();
        final var initiator = UserUtils.randomUser();
        final var acceptorPersonage = PersonageUtils.withId(acceptor.personageId());
        final var initiatorPersonage = PersonageUtils.withId(initiator.personageId());
        final var duel = DuelUtils.withPersonages(initiatorPersonage.id(), acceptorPersonage.id());
        final var command = new DeclineDuel(
            TestRandom.randomNumeric(10),
            group.id(),
            acceptor.id(),
            TestRandom.nextInt(),
            duel.id(),
            TestRandom.randomAlphanumeric(20)
        );

        Mockito.when(duelService.getByIdForce(duel.id())).thenReturn(duel);
        Mockito.when(duelService.declineDuel(duel, acceptorPersonage.id())).thenReturn(Either.right(Success.INSTANCE));
        Mockito.when(userService.getByPersonageIdForce(initiatorPersonage.id())).thenReturn(initiator);
        Mockito.when(personageService.getByIdForce(duel.initiatingPersonageId())).thenReturn(initiatorPersonage);
        final var declineDuelText = TestRandom.randomAlphanumeric(10);

        // when
        try (final var mock = Mockito.mockStatic(DuelLocalization.class)) {
            mock.when(() -> DuelLocalization
                    .declinedDuel(
                        group.language(),
                        TgPersonageMention.of(initiatorPersonage, initiator.id())
                    )
                )
                .thenReturn(declineDuelText);
            executor.processDuel(command, group, acceptor);
        }

        // then
        final var captor = ArgumentCaptor.forClass(EditMessageText.class);
        Mockito.verify(telegramSender).send(captor.capture());
        System.out.println(captor.getValue());
        final var expected = EditMessageText.builder()
            .chatId(group.id().value())
            .messageId(command.messageId())
            .parseMode(ParseMode.HTML)
            .disableWebPagePreview(true)
            .entities(List.of())
            .text(declineDuelText)
            .build();

        Assertions.assertEquals(expected, captor.getValue());
    }
}
