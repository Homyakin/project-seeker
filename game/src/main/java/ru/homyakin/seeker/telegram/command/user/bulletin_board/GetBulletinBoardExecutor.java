package ru.homyakin.seeker.telegram.command.user.bulletin_board;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.personal_quest.PersonalQuestService;
import ru.homyakin.seeker.locale.personal.BulletinBoardLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GetBulletinBoardExecutor extends CommandExecutor<GetBulletinBoard> {
    private final UserService userService;
    private final PersonalQuestService personalQuestService;
    private final TelegramSender telegramSender;

    public GetBulletinBoardExecutor(
        UserService userService,
        PersonalQuestService personalQuestService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.personalQuestService = personalQuestService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GetBulletinBoard command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var requirements = personalQuestService.getRequirements();
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(BulletinBoardLocalization.bulletinBoard(user.language(), requirements))
            .keyboard(ReplyKeyboards.bulletinBoardKeyboard(user.language()))
            .build()
        );
    }

}
