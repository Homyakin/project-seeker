package ru.homyakin.seeker.telegram.command.group.spin;

import com.vdurmont.emoji.EmojiParser;
import java.util.Collections;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.locale.spin.EverydaySpinLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.EverydaySpinService;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.models.SpinError;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class SpinExecutor extends CommandExecutor<Spin> {
    private final EverydaySpinService everydaySpinService;
    private final GroupUserService groupUserService;
    private final PersonageService personageService;
    private final UserService userService;
    private final TelegramSender telegramSender;

    public SpinExecutor(
        EverydaySpinService everydaySpinService,
        GroupUserService groupUserService,
        PersonageService personageService,
        UserService userService,
        TelegramSender telegramSender
    ) {
        this.everydaySpinService = everydaySpinService;
        this.groupUserService = groupUserService;
        this.personageService = personageService;
        this.userService = userService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(Spin command) {
        final var group = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId()).first();
        everydaySpinService.chooseRandomUserId(command.groupId())
            .map(userService::getOrCreateFromGroup)
            .peek(user -> {
                final var personage = personageService.getByIdForce(user.personageId());
                final var text = EmojiParser.parseToUnicode(EverydaySpinLocalization.chosenUser(group.language(), personage));
                telegramSender.send(
                    SendMessage.builder()
                        .chatId(command.groupId())
                        .text(text)
                        .entities(Collections.singletonList(createEntity(text, personage, user.id())))
                        .build()
                );
            })
            .peekLeft(error -> {
                    if (error instanceof SpinError.NotEnoughUsers notEnoughUsers) {
                        telegramSender.send(
                            TelegramMethods.createSendMessage(
                                command.groupId(),
                                EverydaySpinLocalization.notEnoughUsers(group.language(), notEnoughUsers.requiredUsers())
                            )
                        );
                    } else if (error instanceof SpinError.AlreadyChosen alreadyChosen) {
                        final var personage = personageService.getByIdForce(
                            userService.getOrCreateFromGroup(alreadyChosen.userId()).personageId()
                        );
                        final var text = EmojiParser.parseToUnicode(EverydaySpinLocalization.alreadyChosen(group.language(), personage));
                        telegramSender.send(
                            SendMessage.builder()
                                .chatId(command.groupId())
                                .text(text)
                                .entities(Collections.singletonList(createEntity(text, personage, alreadyChosen.userId())))
                                .build()
                        );
                    }
                }
            );
    }

    private MessageEntity createEntity(String text, Personage personage, long userId) {
        final var parsedIcon = EmojiParser.parseToUnicode(personage.icon());
        return MessageEntity
            .builder()
            .type("text_mention")
            .user(new User(userId, "", false))
            .offset(text.indexOf(parsedIcon) + parsedIcon.length())
            .length(personage.name().length())
            .build();
    }
}
