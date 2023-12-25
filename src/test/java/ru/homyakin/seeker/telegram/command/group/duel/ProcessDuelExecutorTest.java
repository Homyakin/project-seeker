package ru.homyakin.seeker.telegram.command.group.duel;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.test_utils.telegram.GroupUtils;
import ru.homyakin.seeker.test_utils.telegram.UserUtils;
import ru.homyakin.seeker.utils.models.Pair;

import java.util.List;

public class ProcessDuelExecutorTest {
    private GroupUserService groupUserService = Mockito.mock(GroupUserService.class);
    private TelegramSender telegramSender = Mockito.mock(TelegramSender.class);
    private ProcessDuelExecutor executor = new TestingProcessDuelExecutor(
        telegramSender,
        groupUserService
    );

    @Test
    @SuppressWarnings("unchecked")
    public void When_ProcessDuelReturnDuelLockedError_Then_SendDuelLockedTextToCallback() {
        // given
        final var group = GroupUtils.randomGroup();
        final var acceptor = UserUtils.randomUser();
        final var command = new AcceptDuel(
            RandomStringUtils.randomNumeric(10),
            group.id(),
            acceptor.id(),
            RandomUtils.nextInt(),
            1,
            RandomStringUtils.randomAlphanumeric(20)
        );
        Mockito.when(groupUserService.getAndActivateOrCreate(group.id(), acceptor.id())).thenReturn(new Pair<>(group, acceptor));
        final var localeText = RandomStringUtils.randomAlphanumeric(10);

        // when
        try (final var mock = Mockito.mockStatic(DuelLocalization.class)) {
            mock.when(() -> DuelLocalization.duelIsLocked(group.language())).thenReturn(localeText);
            executor.execute(command);
        }

        // then
        final var captor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        Mockito.verify(telegramSender).send(captor.capture());
        final var expected = AnswerCallbackQuery.builder()
            .callbackQueryId(command.callbackId())
            .text(localeText)
            .showAlert(true)
            .build();
        Assertions.assertEquals(expected, captor.getValue());

        //TODO прочитать про AnswerCallbackQuery cacheTime=null
    }

    @Test
    @SuppressWarnings("unchecked")
    public void When_ProcessDuelReturnDuelIsFinished_Then_SendDuelIsFinishedTextToCallbackAndEditMessage() {
        // given
        final var group = GroupUtils.randomGroup();
        final var acceptor = UserUtils.randomUser();
        final var command = new AcceptDuel(
            RandomStringUtils.randomNumeric(10),
            group.id(),
            acceptor.id(),
            RandomUtils.nextInt(),
            2,
            RandomStringUtils.randomAlphanumeric(20)
        );
        Mockito.when(groupUserService.getAndActivateOrCreate(group.id(), acceptor.id())).thenReturn(new Pair<>(group, acceptor));
        final var localeText = RandomStringUtils.randomAlphanumeric(10);

        // when
        try (final var mock = Mockito.mockStatic(DuelLocalization.class)) {
            mock.when(() -> DuelLocalization.duelAlreadyFinished(group.language())).thenReturn(localeText);
            executor.execute(command);
        }

        // then
        final var captorCallback = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        final var captorEdit = ArgumentCaptor.forClass(EditMessageText.class);
        Mockito.verify(telegramSender).send(captorCallback.capture());
        Mockito.verify(telegramSender).send(captorEdit.capture());

        final var expectedCallback = AnswerCallbackQuery.builder()
            .callbackQueryId(command.callbackId())
            .text(localeText)
            .showAlert(true)
            .build();
        Assertions.assertEquals(expectedCallback, captorCallback.getValue());

        final var expectedEdit = EditMessageText
            .builder()
            .chatId(group.id().value())
            .messageId(command.messageId())
            .text(command.currentText())
            .parseMode(ParseMode.HTML)
            .disableWebPagePreview(true)
            .entities(List.of())
            .build();
        Assertions.assertEquals(expectedEdit, captorEdit.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void When_ProcessDuelReturnNotDuelAcceptor_Then_SendNotDuelAcceptorTextToCallback() {
        // given
        final var group = GroupUtils.randomGroup();
        final var acceptor = UserUtils.randomUser();
        final var command = new AcceptDuel(
            RandomStringUtils.randomNumeric(10),
            group.id(),
            acceptor.id(),
            RandomUtils.nextInt(),
            3,
            RandomStringUtils.randomAlphanumeric(20)
        );
        Mockito.when(groupUserService.getAndActivateOrCreate(group.id(), acceptor.id())).thenReturn(new Pair<>(group, acceptor));
        final var localeText = RandomStringUtils.randomAlphanumeric(10);

        // when
        try (final var mock = Mockito.mockStatic(DuelLocalization.class)) {
            mock.when(() -> DuelLocalization.notDuelAcceptingPersonage(group.language())).thenReturn(localeText);
            executor.execute(command);
        }

        // then
        final var captor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);
        Mockito.verify(telegramSender).send(captor.capture());

        final var expectedCallback = AnswerCallbackQuery.builder()
            .callbackQueryId(command.callbackId())
            .text(localeText)
            .showAlert(true)
            .build();
        Assertions.assertEquals(expectedCallback, captor.getValue());
    }
}
