package ru.homyakin.seeker.telegram.command.user.bulletin_board;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.personal_quest.PersonalQuestService;
import ru.homyakin.seeker.game.event.personal_quest.model.TakeQuestError;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.personal.BulletinBoardLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class TakePersonalQuestExecutor extends CommandExecutor<TakePersonalQuest> {
    private final UserService userService;
    private final PersonalQuestService personalQuestService;
    private final TelegramSender telegramSender;

    public TakePersonalQuestExecutor(
        UserService userService,
        PersonalQuestService personalQuestService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.personalQuestService = personalQuestService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(TakePersonalQuest command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        if (command.count().isEmpty()) {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(user.id())
                .text(BulletinBoardLocalization.incorrectQuestCount(user.language()))
                .build()
            );
            return;
        }
        final var text = personalQuestService.takeQuest(user.personageId(), command.count().get())
            .fold(
                error -> switch (error) {
                    case TakeQuestError.NoQuests _, TakeQuestError.PersonageLocked _
                        -> CommonLocalization.internalError(user.language());
                    case TakeQuestError.NotEnoughEnergy notEnoughEnergy ->
                        BulletinBoardLocalization.notEnoughEnergy(user.language(), notEnoughEnergy.requiredEnergy());
                    case TakeQuestError.PersonageInOtherEvent _ ->
                        BulletinBoardLocalization.personageInAnotherEvent(user.language());
                    case TakeQuestError.NotPositiveCount _ ->
                        BulletinBoardLocalization.incorrectQuestCount(user.language());
                },
                startedQuest -> BulletinBoardLocalization.startedQuest(user.language(), startedQuest)
            );
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .build()
        );
    }

}
