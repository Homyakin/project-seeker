package ru.homyakin.seeker.telegram.command.common.world_raid;

import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.homyakin.seeker.RandomUtilsTest;
import ru.homyakin.seeker.game.event.world_raid.action.WorldRaidContributionService;
import ru.homyakin.seeker.game.event.world_raid.entity.WorldRaidResearchDonateError;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocalizationInitializer;
import ru.homyakin.seeker.locale.personal.BulletinBoardLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.test_utils.TestRandom;
import ru.homyakin.seeker.test_utils.telegram.GroupUtils;
import ru.homyakin.seeker.test_utils.telegram.UserUtils;

import static org.mockito.ArgumentMatchers.any;

public class WorldRaidDonateExecutorTest {
    private final UserService userService = Mockito.mock();
    private final GroupTgService groupTgService = Mockito.mock();
    private final WorldRaidContributionService worldRaidContributionService = Mockito.mock();
    private final TelegramSender telegramSender = Mockito.mock();
    private final WorldRaidDonateExecutor executor = new WorldRaidDonateExecutor(
        userService,
        groupTgService,
        worldRaidContributionService,
        telegramSender
    );

    @BeforeAll
    public static void initLocale() {
        LocalizationInitializer.initLocale();
    }

    @Test
    public void When_SuccessDonateFromPrivate_Then_UseUserLanguageForSuccess() {
        final var user = UserUtils.randomUser();
        final var command = new WorldRaidDonate(user.id().value(), true, user.id());
        final var money = Money.from(TestRandom.nextInt());

        Mockito.when(userService.forceGetFromPrivate(user.id())).thenReturn(user);
        Mockito.when(worldRaidContributionService.donate(any())).thenReturn(Either.right(money));

        final var argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.when(telegramSender.send(argumentCaptor.capture())).thenReturn(Either.right(null));

        executor.execute(command);

        Mockito.verify(groupTgService, Mockito.never()).getOrCreate(any());
        Assertions.assertEquals(
            BulletinBoardLocalization.successWorldRaidDonate(user.language(), money),
            argumentCaptor.getValue().getText()
        );
        Assertions.assertEquals(
            user.id().value(),
            Long.valueOf(argumentCaptor.getValue().getChatId())
        );
    }

    @Test
    public void When_UnSuccessDonateFromPrivate_Then_UseUserLanguageForError() {
        final var user = UserUtils.randomUser();
        final var command = new WorldRaidDonate(user.id().value(), true, user.id());

        Mockito.when(userService.forceGetFromPrivate(user.id())).thenReturn(user);
        Mockito.when(worldRaidContributionService.donate(any()))
            .thenReturn(Either.left(WorldRaidResearchDonateError.ResearchCompleted.INSTANCE));

        final var argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.when(telegramSender.send(argumentCaptor.capture())).thenReturn(Either.right(null));

        executor.execute(command);

        Mockito.verify(groupTgService, Mockito.never()).getOrCreate(any());
        Assertions.assertEquals(
            BulletinBoardLocalization.worldRaidDonateError(
                user.language(),
                WorldRaidResearchDonateError.ResearchCompleted.INSTANCE
            ),
            argumentCaptor.getValue().getText()
        );
        Assertions.assertEquals(
            user.id().value(),
            Long.valueOf(argumentCaptor.getValue().getChatId())
        );
    }

    @Test
    public void When_SuccessDonateFromGroup_Then_UseGroupLanguageForError() {
        final var user = UserUtils.randomUser();
        final var group = GroupUtils.randomWithLanguage(Language.KZ);
        final var command = new WorldRaidDonate(group.id().value(), false, user.id());

        Mockito.when(userService.forceGetFromPrivate(user.id())).thenReturn(user);
        Mockito.when(worldRaidContributionService.donate(any()))
            .thenReturn(Either.left(WorldRaidResearchDonateError.ResearchCompleted.INSTANCE));
        Mockito.when(groupTgService.getOrCreate(group.id())).thenReturn(group);

        final var argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.when(telegramSender.send(argumentCaptor.capture())).thenReturn(Either.right(null));

        executor.execute(command);

        Assertions.assertEquals(
            BulletinBoardLocalization.worldRaidDonateError(
                group.language(),
                WorldRaidResearchDonateError.ResearchCompleted.INSTANCE
            ),
            argumentCaptor.getValue().getText()
        );
        Assertions.assertEquals(
            group.id().value(),
            Long.valueOf(argumentCaptor.getValue().getChatId())
        );
    }
}
