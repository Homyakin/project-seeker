package ru.homyakin.seeker.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.homyakin.seeker.telegram.command.CommandParser;
import ru.homyakin.seeker.telegram.command.CommandProcessor;
import ru.homyakin.seeker.telegram.user.action.CreateUserOrActualizeUsernameCommand;
import ru.homyakin.seeker.telegram.user.entity.UserRequest;
import ru.homyakin.seeker.telegram.user.entity.Username;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.telegram.utils.TelegramUtils;

import java.util.Optional;

@Component
public class TelegramUpdateReceiver implements LongPollingSingleThreadUpdateConsumer {
    private static final Logger logger = LoggerFactory.getLogger(TelegramUpdateReceiver.class);

    private final TelegramBotConfig config;
    private final CommandParser commandParser;
    private final CommandProcessor commandProcessor;
    private final CreateUserOrActualizeUsernameCommand createUserOrActualizeUsernameCommand;

    public TelegramUpdateReceiver(
        TelegramBotConfig config,
        CommandParser commandParser,
        CommandProcessor commandProcessor,
        CreateUserOrActualizeUsernameCommand createUserOrActualizeUsernameCommand
    ) {
        this.config = config;
        this.commandParser = commandParser;
        this.commandProcessor = commandProcessor;
        this.createUserOrActualizeUsernameCommand = createUserOrActualizeUsernameCommand;
    }

    @Override
    public void consume(Update update) {
        logger.debug("New update: {}", update.toString());
        try {
            updateSavedUserData(update);
            if (TelegramUtils.needToProcessUpdate(update, config.username())) {
                commandParser.parse(update).ifPresent(commandProcessor::process);
            }
        } catch (Exception e) {
            logger.error("Unknown error during update processing", e);
        }
    }

    private void updateSavedUserData(Update update) {
        if (update.hasMessage()) {
            updateSavedUserData(update.getMessage().getFrom());
            if (update.getMessage().isReply()) {
                updateSavedUserData(update.getMessage().getReplyToMessage().getFrom());
            }
        } else if (update.hasCallbackQuery()) {
            updateSavedUserData(update.getCallbackQuery().getFrom());
        } else if (update.hasChatMember()) {
            if (update.getChatMember().getNewChatMember() != null) {
                updateSavedUserData(update.getChatMember().getNewChatMember().getUser());
            }
        }
    }

    private void updateSavedUserData(User user) {
        final var username = Optional.ofNullable(user.getUserName());
        final var request = new UserRequest(
            UserId.from(user.getId()),
            username,
            username.map(Username::from)
        );
        createUserOrActualizeUsernameCommand.execute(request);
    }
}
