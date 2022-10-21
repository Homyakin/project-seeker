package ru.homyakin.seeker.telegram.command.chat.top;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.chat.ChatUserService;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class TopExecutor extends CommandExecutor<Top> {
    private final ChatUserService chatUserService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public TopExecutor(
        ChatUserService chatUserService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.chatUserService = chatUserService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(Top command) {
        final var result = chatUserService.getAndActivateOrCreate(command.chatId(), command.userId());
        final var chat = result.first();
        final var user = result.second();
        final var topPersonages = personageService.getTopByExpInChat(chat.id(), 10);
        final var containsUser = topPersonages.stream().anyMatch(personage -> personage.id() == user.personageId());
        final StringBuilder topText = new StringBuilder();
        for (int i = 0; i < topPersonages.size(); ++i) {
            topText.append(i + 1).append(". ").append(topPersonages.get(i).toTopText()).append("\n");
        }
        if (!containsUser) {
            personageService.getPersonagePositionInTopByExpInChat(user.personageId(), chat.id()).ifPresent(position ->
                personageService.getById(user.personageId())
                    .map(Personage::toTopText)
                    .ifPresent(text -> {
                            topText.append(".........\n").append(position).append(". ").append(text);
                        }
                    )
            );
        }
        telegramSender.send(
            TelegramMethods.createSendMessage(
                chat.id(),
                Localization.get(chat.language()).topPersonagesByExpInChat().formatted(topText)
            )
        );
    }
}
