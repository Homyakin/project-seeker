package ru.homyakin.seeker.telegram.command.user.personal_quest;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.personal_quest.PersonalQuestService;
import ru.homyakin.seeker.game.event.personal_quest.model.TakeQuestError;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.personal.PersonalQuestLocalization;
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
        final var user = userService.getOrCreateFromPrivate(command.userId());
        final var text = personalQuestService.takeQuest(user.personageId())
            .fold(
                error -> switch (error) {
                    case TakeQuestError.NoQuests _, TakeQuestError.PersonageLocked _
                        -> CommonLocalization.internalError(user.language());
                    case TakeQuestError.NotEnoughEnergy notEnoughEnergy ->
                        PersonalQuestLocalization.notEnoughEnergy(user.language(), notEnoughEnergy.requiredEnergy());
                    case TakeQuestError.PersonageInOtherEvent _ -> PersonalQuestLocalization.personageInAnotherEvent(user.language());
                },
                startedQuest -> PersonalQuestLocalization.startedQuest(user.language(), startedQuest)
            );
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .build()
        );
    }

}
