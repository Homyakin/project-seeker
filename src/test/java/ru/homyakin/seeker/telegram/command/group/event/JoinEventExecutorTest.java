package ru.homyakin.seeker.telegram.command.group.event;

import io.vavr.control.Either;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.game.event.service.EventService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.locale.raid.RaidLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.InlineKeyboardBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.test_utils.event.EventUtils;
import ru.homyakin.seeker.test_utils.event.LaunchedEventUtils;
import ru.homyakin.seeker.test_utils.telegram.GroupUtils;
import ru.homyakin.seeker.test_utils.telegram.UserUtils;
import ru.homyakin.seeker.utils.models.Pair;

import java.util.List;
import java.util.Optional;

public class JoinEventExecutorTest {
    private final GroupUserService groupUserService = Mockito.mock(GroupUserService.class);
    private final PersonageService personageService = Mockito.mock(PersonageService.class);
    private final TelegramSender telegramSender = Mockito.mock(TelegramSender.class);
    private final EventService eventService = Mockito.mock(EventService.class);
    private final JoinEventExecutor executor = new JoinEventExecutor(
        groupUserService,
        personageService,
        telegramSender,
        eventService
    );
    private Group group;
    private User user;
    private JoinEvent command;
    private Event event;
    private LaunchedEvent launchedEvent;

    @BeforeEach
    public void init() {
        group = GroupUtils.randomGroup();
        user = UserUtils.randomUser();
        event = EventUtils.randomEvent();
        launchedEvent = LaunchedEventUtils.fromEvent(event);
        command = new JoinEvent(
            RandomStringUtils.randomNumeric(10),
            group.id(),
            RandomUtils.nextInt(),
            user.id(),
            launchedEvent.id()
        );
        Mockito.when(groupUserService.getAndActivateOrCreate(group.id(), user.id())).thenReturn(new Pair<>(group, user));
    }

    @Test
    public void When_JoinEventIsSuccess_Then_EditTelegramMessage() {
        // given
        Mockito.when(personageService.addEvent(user.personageId(), launchedEvent.id())).thenReturn(Either.right(launchedEvent));
        final var participants = List.of(PersonageUtils.random(), PersonageUtils.withId(user.personageId()));
        Mockito.when(personageService.getByLaunchedEvent(launchedEvent.id()))
            .thenReturn(participants);
        Mockito.when(eventService.getEventById(launchedEvent.eventId())).thenReturn(Optional.of(event));

        final var participantsText = RandomStringUtils.random(30);
        final var hours = RandomStringUtils.random(2);
        final var minutes = RandomStringUtils.random(2);
        final var timePrefix = RandomStringUtils.random(10);
        final var keyboard = InlineKeyboardBuilder
            .builder()
            .addRow()
            .addButton(
                RandomStringUtils.random(10),
                RandomStringUtils.randomAlphanumeric(10)
            )
            .build();

        // when
        try (
            final var raid = Mockito.mockStatic(RaidLocalization.class);
            final var keyboardMock = Mockito.mockStatic(InlineKeyboards.class)
        ) {
            raid.when(() -> RaidLocalization.raidParticipants(group.language(), participants)).thenReturn(participantsText);
            raid.when(() -> RaidLocalization.hoursShort(group.language())).thenReturn(hours);
            raid.when(() -> RaidLocalization.minutesShort(group.language())).thenReturn(minutes);
            raid.when(() -> RaidLocalization.raidStartsPrefix(group.language())).thenReturn(timePrefix);
            keyboardMock.when(() -> InlineKeyboards.joinRaidEventKeyboard(group.language(), launchedEvent.id())).thenReturn(keyboard);
            executor.execute(command);
        }

        //then
        final var captor = ArgumentCaptor.forClass(EditMessageText.class);
        Mockito.verify(telegramSender).send(captor.capture());

        final var timeText = timePrefix + " "
            + event.duration().toHours() + " " + hours + " "
            + event.duration().toMinutesPart() + " " + minutes;
        final var expectedText = "<b>%s</b>%n%n%s".formatted(
            LocaleUtils.getLocaleByLanguageOrDefault(event.locales(), group.language()).get().intro(),
            LocaleUtils.getLocaleByLanguageOrDefault(event.locales(), group.language()).get().description()
        ) + "\n\n" + timeText + "\n\n" + participantsText;

        final var expected = EditMessageText.builder()
            .chatId(group.id().value())
            .messageId(command.messageId())
            .parseMode(ParseMode.HTML)
            .disableWebPagePreview(true)
            .entities(List.of())
            .text(expectedText)
            .replyMarkup(keyboard)
            .build();

        Assertions.assertEquals(expected, captor.getValue());
    }
}
