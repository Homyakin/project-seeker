package ru.homyakin.seeker.telegram.command.chat.profile;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.chat.ChatUserService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class GetProfileInChatExecutor extends CommandExecutor<GetProfileInChat> {
    private final ChatUserService chatUserService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public GetProfileInChatExecutor(
        ChatUserService chatUserService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.chatUserService = chatUserService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GetProfileInChat command) {
        final var chatUserPair = chatUserService.getAndActivateOrCreate(
            command.chatId(),
            command.userId()
        );
        final var chat = chatUserPair.first();
        final var user = chatUserPair.second();
        final var personage = personageService
            .getById(user.personageId())
            .orElseThrow(() -> new IllegalStateException("Personage must be present at user with id " + user))
            ;

        telegramSender.send(
            TelegramMethods.createSendMessage(command.chatId(), personage.toProfile(chat.language()))
        );
    }

}
